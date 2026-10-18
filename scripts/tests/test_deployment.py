import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from types import SimpleNamespace
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('deployment_services', ROOT / 'scripts/local_services.py')
services = importlib.util.module_from_spec(spec)
spec.loader.exec_module(services)


class DeploymentTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.state_patch = patch.object(services, 'STATE', Path(self.directory.name))
        self.state_patch.start()
        self.sse_type = 'text/event-stream'
        self.api_working = True
        test = self

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                status, content_type = 200, 'application/json'
                if self.path == '/sse':
                    body = b''
                    content_type = test.sse_type
                elif self.path == '/':
                    body, content_type = b'<div id="app"></div>', 'text/html'
                elif self.path == '/milvus/health':
                    body = json.dumps({'message': 'ok'}).encode()
                elif self.path == '/api/chat/session/deploy-readiness' and test.api_working:
                    body = json.dumps({'message': '会话不存在'}).encode()
                else:
                    status, body = 404, b'{}'
                self.send_response(status)
                self.send_header('Content-Type', content_type)
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, *args):
                pass

        self.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = 'http://127.0.0.1:' + str(self.server.server_port)

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)
        self.state_patch.stop()
        self.directory.cleanup()

    def test_deployment_refuses_an_external_backend_without_starting_or_stopping_it(self):
        with patch.object(services.subprocess, 'Popen') as launch, patch.object(services.os, 'killpg') as kill:
            with self.assertRaisesRegex(RuntimeError, 'external process'):
                services.start('agent', self.url, self.url + '/sse')
            launch.assert_not_called()
            kill.assert_not_called()

    def test_mcp_readiness_requires_sse_not_just_a_listening_port(self):
        self.assertTrue(services.ready('mcp', self.url, self.url + '/sse'))
        self.sse_type = 'text/plain'
        self.assertFalse(services.ready('mcp', self.url, self.url + '/sse'))

    def test_frontend_readiness_checks_api_forwarding_as_well_as_static_files(self):
        self.assertTrue(services.ready('web', self.url, self.url + '/sse', self.url))
        self.api_working = False
        self.assertFalse(services.ready('web', self.url, self.url + '/sse', self.url))

    def test_exited_backend_fails_immediately_instead_of_waiting_for_the_deadline(self):
        record = Path(self.directory.name) / 'agent.json'
        record.write_text(json.dumps({'pid': 2147483647, 'started_at': 'exited'}))
        started = time.monotonic()
        with self.assertRaisesRegex(RuntimeError, 'exited during startup'):
            services.wait_ready('agent', self.url, self.url + '/sse', self.url, 180)
        self.assertLess(time.monotonic() - started, 1)

    def test_jar_start_passes_the_health_check_port_to_spring_boot(self):
        with patch.object(services, 'preflight'), patch.object(services, 'identity', return_value='birth'), \
                patch.object(services.subprocess, 'Popen', return_value=SimpleNamespace(pid=12345)) as launch:
            services.start('agent', 'http://127.0.0.1:19900', self.url + '/sse')
            self.assertEqual(launch.call_args.args[0][-2:], ['--jar', '--server.port=19900'])
            self.assertTrue(launch.call_args.kwargs['start_new_session'])

    def test_frontend_start_uses_vite_preview_and_the_actual_backend_url(self):
        root = Path(self.directory.name)
        (root / 'frontend/dist').mkdir(parents=True)
        (root / 'frontend/dist/index.html').write_text('<div id="app"></div>')
        with patch.object(services, 'ROOT', root), patch.object(services, 'preflight'), \
                patch.object(services, 'identity', return_value='birth'), \
                patch.object(services.subprocess, 'Popen', return_value=SimpleNamespace(pid=12345)) as launch:
            services.start('web', 'http://127.0.0.1:19900', self.url + '/sse',
                           mode='preview', web_url='http://127.0.0.1:18080')
            command = launch.call_args.args[0]
            self.assertIn('preview', command)
            self.assertIn('18080', command)
            self.assertIn('--strictPort', command)
            self.assertEqual(launch.call_args.kwargs['env']['API_PROXY_TARGET'], 'http://127.0.0.1:19900')

    def test_failed_readiness_cleans_up_the_newly_started_process(self):
        with patch.object(services, 'settings', return_value={}), \
                patch.object(sys, 'argv', ['services', 'start', 'agent']), \
                patch.object(services, 'start') as start, \
                patch.object(services, 'wait_ready', side_effect=RuntimeError('readiness failed')), \
                patch.object(services, 'stop') as stop:
            with self.assertRaisesRegex(RuntimeError, 'readiness failed'):
                services.main()
            start.assert_called_once()
            stop.assert_called_once_with('agent')

    def test_mock_mode_does_not_start_a_local_mcp(self):
        with patch.object(services, 'settings', return_value={'LOGS_MOCK_ENABLED': 'true'}), \
                patch.object(sys, 'argv', ['services', 'ensure', 'mcp']), \
                patch.object(services, 'start') as launch:
            self.assertEqual(services.main(), 0)
            launch.assert_not_called()


class ConfigurationTest(unittest.TestCase):
    def test_project_env_and_explicit_port_override_are_consistent_without_returning_secrets(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / '.env'
            file.write_text('DASHSCOPE_API_KEY=private-test-value\nSERVER_PORT=19901\nWEB_PORT=18080\n')
            with patch.dict(os.environ, {'ONCALL_ENV_FILE': str(file)}, clear=True):
                values = services.settings()
                self.assertNotIn('DASHSCOPE_API_KEY', values)
                self.assertEqual(services.endpoints(values)[0], 'http://127.0.0.1:19901')
                self.assertEqual(services.endpoints(values)[2], 'http://127.0.0.1:18080')
            with patch.dict(os.environ, {'ONCALL_ENV_FILE': str(file), 'SERVER_PORT': '19902'}, clear=True):
                self.assertEqual(services.endpoints(services.settings())[0], 'http://127.0.0.1:19902')

    def test_deployment_plan_builds_then_starts_jar_and_web_without_importing_documents(self):
        result = subprocess.run(['make', '-n', 'deploy'], cwd=ROOT, capture_output=True, text=True, timeout=10)
        self.assertEqual(result.returncode, 0, result.stderr)
        plan = result.stdout
        self.assertLess(plan.index('preflight agent'), plan.index('mvn -DskipTests package'))
        self.assertLess(plan.index('mvn -DskipTests package'), plan.index('restart agent --mode jar'))
        self.assertLess(plan.index('restart agent --mode jar'), plan.index('restart web --mode preview'))
        self.assertNotIn('nginx', plan.lower())
        self.assertNotIn('docker compose', plan)
        self.assertNotIn('upload_docs.py', plan)


if __name__ == '__main__':
    unittest.main()
