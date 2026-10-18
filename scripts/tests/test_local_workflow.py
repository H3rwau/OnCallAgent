import importlib.util
import json
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('local_services', ROOT / 'scripts/local_services.py')
services = importlib.util.module_from_spec(spec)
spec.loader.exec_module(services)


class ImportDocsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.path = Path(self.directory.name) / 'runbook.md'
        self.path.write_text('# Runbook\n测试知识文档\n')
        self.status = 200
        self.payload = {'code': 200, 'data': {'fileName': self.path.name,
                         'fileSize': self.path.stat().st_size, 'filePath': './uploads/runbook.md'}}
        self.requests = []
        test = self

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                test.requests.append((self.path, self.headers.get('Content-Type'),
                                      self.rfile.read(int(self.headers['Content-Length']))))
                body = json.dumps(test.payload).encode()
                self.send_response(test.status)
                self.send_header('Content-Type', 'application/json')
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
        self.directory.cleanup()

    def run_import(self, *extra):
        return subprocess.run([sys.executable, str(ROOT / 'scripts/upload_docs.py'),
                               '--directory', self.directory.name, '--api-url', self.url, *extra],
                              capture_output=True, text=True, timeout=5)

    def test_multipart_import_validates_the_backend_acknowledgement(self):
        result = self.run_import()
        self.assertEqual(result.returncode, 0, result.stderr)
        path, content_type, body = self.requests[0]
        self.assertEqual(path, '/api/upload')
        self.assertIn('multipart/form-data; boundary=', content_type)
        self.assertIn(b'name="file"; filename="runbook.md"', body)
        self.assertIn(self.path.read_bytes(), body)

    def test_http_errors_business_errors_and_incomplete_receipts_fail(self):
        for status, payload in [(500, {'code': 500, 'message': '索引失败'}),
                                (200, {'code': 500, 'message': '索引失败'}),
                                (200, {'code': 200, 'data': {}})]:
            with self.subTest(status=status, payload=payload):
                self.status, self.payload = status, payload
                self.assertNotEqual(self.run_import().returncode, 0)

    def test_batch_import_requires_a_nonempty_explicit_directory(self):
        for args in [[], ['--directory', '']]:
            with self.subTest(args=args):
                result = subprocess.run([sys.executable, str(ROOT / 'scripts/upload_docs.py'),
                                         '--api-url', self.url, *args],
                                        capture_output=True, text=True, timeout=5)
                self.assertEqual(result.returncode, 2)
        self.assertEqual(self.requests, [])

    def test_dry_run_does_not_send_requests(self):
        self.assertEqual(self.run_import('--dry-run').returncode, 0)
        self.assertEqual(self.requests, [])


class ProcessOwnershipTest(unittest.TestCase):
    def test_locale_and_timezone_cannot_change_process_ownership(self):
        process = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], start_new_session=True)
        try:
            with patch.dict(services.os.environ, {'LC_ALL': 'en_SG.UTF-8', 'TZ': 'UTC-8'}):
                record = {'pid': process.pid, 'started_at': services.identity(process.pid)}
            with patch.dict(services.os.environ, {'LC_ALL': 'C', 'TZ': 'UTC'}):
                self.assertTrue(services.owned_process(record))
        finally:
            process.terminate()
            process.wait(timeout=3)

    def test_stale_process_record_cannot_signal_a_reused_pid(self):
        with tempfile.TemporaryDirectory() as directory, patch.object(services, 'STATE', Path(directory)):
            record = Path(directory) / 'agent.json'
            record.write_text(json.dumps({'pid': __import__('os').getpid(), 'started_at': 'old process'}))
            with patch.object(services.os, 'killpg') as kill:
                with self.assertRaisesRegex(RuntimeError, 'record retained'):
                    services.stop('agent')
                kill.assert_not_called()
            self.assertTrue(record.exists())

    def test_owned_service_is_stopped_as_a_group(self):
        process = subprocess.Popen([sys.executable, '-c', 'import time; time.sleep(30)'], start_new_session=True)
        try:
            with tempfile.TemporaryDirectory() as directory, patch.object(services, 'STATE', Path(directory)):
                record = Path(directory) / 'agent.json'
                record.write_text(json.dumps({'pid': process.pid, 'started_at': services.identity(process.pid)}))
                services.stop('agent')
                self.assertEqual(process.wait(timeout=3), -signal.SIGTERM)
                self.assertFalse(record.exists())
        finally:
            if process.poll() is None:
                process.terminate()
                process.wait(timeout=3)


if __name__ == '__main__':
    unittest.main()
