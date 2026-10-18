import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import unittest
import zipfile
from email import policy
from email.parser import BytesParser
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parents[2]


class OfflineKnowledgeTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.root = Path(self.directory.name)
        self.resources = self.root / 'resources'
        (self.resources / 'cases').mkdir(parents=True)
        self.records = []
        for case_id in ['OMR-C01', 'OMR-C02']:
            content = ('# ' + case_id + '\n\n故障案例与验证边界。\n').encode()
            filename = 'cases/' + case_id + '.md'
            (self.resources / filename).write_bytes(content)
            self.records.append({'id': case_id, 'file': filename, 'title': case_id,
                                 'sha256': hashlib.sha256(content).hexdigest()})
        self.manifest = {'schemaVersion': 1, 'id': 'omr-cases', 'total': 2, 'cases': self.records}
        self.save_manifest()
        (self.resources / 'README.md').write_text('# Offline resources\n')
        self.requests = []
        self.fail_files = set()
        test = self

        class Handler(BaseHTTPRequestHandler):
            def do_POST(self):
                raw = self.rfile.read(int(self.headers['Content-Length']))
                message = BytesParser(policy=policy.default).parsebytes(
                    ('Content-Type: ' + self.headers['Content-Type'] + '\r\nMIME-Version: 1.0\r\n\r\n').encode() + raw)
                parts = list(message.iter_parts())
                if len(parts) != 1 or parts[0].get_param('name', header='content-disposition') != 'file':
                    self.send_error(400)
                    return
                part = parts[0]
                filename = part.get_filename()
                test.requests.append((self.path, filename, part.get_payload(decode=True)))
                if filename in test.fail_files:
                    status, payload = 500, {'code': 500, 'message': '向量索引失败'}
                else:
                    status, payload = 200, {'code': 200, 'data': {'fileName': filename,
                                           'fileSize': len(part.get_payload(decode=True)),
                                           'filePath': './uploads/' + filename}}
                body = json.dumps(payload).encode()
                self.send_response(status)
                self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, *args):
                pass

        self.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.url = 'http://127.0.0.1:' + str(self.server.server_port) + '/stage'

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)
        self.directory.cleanup()

    def save_manifest(self):
        (self.resources / 'manifest.json').write_text(json.dumps(self.manifest))

    def run_import(self, *args):
        return subprocess.run([sys.executable, str(ROOT / 'scripts/import_knowledge.py'),
                               '--resource-dir', str(self.resources), '--api-url', self.url, *args],
                              cwd=self.root, capture_output=True, text=True, timeout=10)

    def test_dry_run_from_an_unrelated_directory_is_read_only(self):
        result = self.run_import('--dry-run')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(result.stdout.count('待导入:'), 2)
        self.assertEqual(self.requests, [])

    def test_all_resources_are_checked_before_any_network_write(self):
        (self.resources / 'cases/OMR-C02.md').write_text('# Changed without updating manifest')
        result = self.run_import()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('SHA-256', result.stderr)
        self.assertEqual(self.requests, [])

    def test_manifest_cannot_upload_a_file_outside_the_resource_directory(self):
        self.records[0]['file'] = '../../OMR-C01.md'
        self.save_manifest()
        self.assertNotEqual(self.run_import().returncode, 0)
        self.assertEqual(self.requests, [])

    def test_selected_case_uses_the_target_application_path(self):
        result = self.run_import('--case', 'OMR-C02')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([(path, name) for path, name, _ in self.requests],
                         [('/stage/api/upload', 'OMR-C02.md')])

    def test_failure_report_and_selective_retry(self):
        self.fail_files.add('OMR-C02.md')
        report = self.root / 'report.json'
        first = self.run_import('--report', str(report))
        self.assertEqual(first.returncode, 1)
        payload = json.loads(report.read_text())
        self.assertEqual(payload['failed'], ['OMR-C02'])
        self.assertEqual([result['status'] for result in payload['results']], ['success', 'failed'])
        self.fail_files.clear()
        retry = self.run_import('--case', 'OMR-C02')
        self.assertEqual(retry.returncode, 0, retry.stderr)
        self.assertEqual([name for _, name, _ in self.requests], ['OMR-C01.md', 'OMR-C02.md', 'OMR-C02.md'])

    def add_reference(self):
        folder = self.resources / 'references'
        folder.mkdir()
        path = folder / 'OMR-REF-ENGINE-CODES.md'
        content = '# OMR 原生错误码\n\n码 20 的含义需要限定所属层级。\n'.encode()
        path.write_bytes(content)
        self.manifest.update(schemaVersion=2, total=3, caseCount=2, referenceCount=1,
                             references=[{'id': 'OMR-REF-ENGINE-CODES', 'title': '原生错误码',
                                          'file': 'references/' + path.name,
                                          'sha256': hashlib.sha256(content).hexdigest()}])
        self.save_manifest()

    def test_mixed_manifest_imports_both_cases_and_reference_documents(self):
        self.add_reference()
        result = self.run_import()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual([name for _, name, _ in self.requests],
                         ['OMR-C01.md', 'OMR-C02.md', 'OMR-REF-ENGINE-CODES.md'])

    def test_reference_selection_and_resource_id_alias(self):
        self.add_reference()
        for arguments in [('--kind', 'references'), ('--id', 'OMR-REF-ENGINE-CODES')]:
            with self.subTest(arguments=arguments):
                self.requests.clear()
                result = self.run_import(*arguments)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual([name for _, name, _ in self.requests], ['OMR-REF-ENGINE-CODES.md'])

    def test_reference_integrity_is_checked_even_when_only_cases_are_selected(self):
        self.add_reference()
        (self.resources / 'references/OMR-REF-ENGINE-CODES.md').write_text('# Unsynced edit')
        result = self.run_import('--kind', 'cases')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('SHA-256', result.stderr)
        self.assertEqual(self.requests, [])

    def test_packaged_resources_work_after_moving_without_the_project_or_env_file(self):
        self.add_reference()
        (self.resources / 'SOURCE-COVERAGE.md').write_text('# 来源覆盖\n\n仅用于人工检查，不上传。\n')
        output = self.root / 'offline.zip'
        packaged = self.run_import('--package', str(output))
        self.assertEqual(packaged.returncode, 0, packaged.stderr)
        destination = self.root / 'different-machine'
        with zipfile.ZipFile(output) as archive:
            members = set(archive.namelist())
            self.assertIn('scripts/import_knowledge.py', members)
            self.assertIn('scripts/upload_docs.py', members)
            self.assertIn('knowledge/omr/manifest.json', members)
            self.assertIn('knowledge/omr/references/OMR-REF-ENGINE-CODES.md', members)
            self.assertIn('knowledge/omr/SOURCE-COVERAGE.md', members)
            self.assertFalse(any('.env' in name or name.startswith('/') for name in members))
            archive.extractall(destination)
        result = subprocess.run([sys.executable, str(destination / 'scripts/import_knowledge.py'),
                                 '--api-url', self.url], cwd=self.root,
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(self.requests), 3)

    def test_make_imports_the_project_resources_to_an_explicit_backend(self):
        result = subprocess.run(['make', 'knowledge-import', 'API_BASE_URL=' + self.url], cwd=ROOT,
                                capture_output=True, text=True, timeout=20)
        self.assertEqual(result.returncode, 0, result.stderr)
        manifest = json.loads((ROOT / 'knowledge/omr/manifest.json').read_text())
        self.assertEqual(len(self.requests), manifest['total'])
        self.assertTrue(all(path == '/stage/api/upload' for path, _, _ in self.requests))


if __name__ == '__main__':
    unittest.main()
