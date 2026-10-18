#!/usr/bin/env python3
"""Validate, package or import the project's bundled OMR knowledge resources."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import urllib.parse
import zipfile

from upload_docs import upload

ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = ROOT / 'knowledge/omr'


def load_resources(directory):
    directory = directory.resolve()
    manifest = json.loads((directory / 'manifest.json').read_text(encoding='utf-8'))
    if not isinstance(manifest, dict) or manifest.get('schemaVersion') not in (1, 2):
        raise ValueError('不支持的知识资源清单格式')
    cases = manifest.get('cases')
    references = manifest.get('references', []) if manifest['schemaVersion'] == 2 else []
    if not isinstance(cases, list) or not isinstance(references, list):
        raise ValueError('知识资源清单必须包含案例和参考资料列表')
    records = [('case', item) for item in cases] + [('reference', item) for item in references]
    if not records or manifest.get('total') != len(records):
        raise ValueError('知识资源数量与清单不一致')
    if (manifest.get('caseCount', len(cases)) != len(cases)
            or manifest.get('referenceCount', len(references)) != len(references)):
        raise ValueError('案例或参考资料数量与清单不一致')
    ids, filenames = set(), set()
    resources = []
    for kind, item in records:
        if not isinstance(item, dict):
            raise ValueError('知识资源清单条目格式错误')
        record = dict(item, kind=kind)
        if not isinstance(record.get('title'), str) or not record['title'].strip():
            raise ValueError('知识资源缺少有效标题')
        resource_id, filename, checksum = record.get('id'), record.get('file'), record.get('sha256')
        pattern = r'OMR-[A-Z][0-9]{2}' if kind == 'case' else r'OMR-REF-[A-Z0-9]+(?:-[A-Z0-9]+)*'
        if not isinstance(resource_id, str) or not re.fullmatch(pattern, resource_id) or resource_id in ids:
            raise ValueError('资源编号格式错误或重复')
        if not isinstance(filename, str) or not isinstance(checksum, str) or not re.fullmatch(r'[a-f0-9]{64}', checksum):
            raise ValueError(f'{resource_id}: 文件路径或校验值格式错误')
        path = (directory / filename).resolve()
        expected_folder = 'cases' if kind == 'case' else 'references'
        if (not path.is_relative_to(directory) or path.suffix.lower() != '.md'
                or Path(filename).parts[0] != expected_folder
                or path.name != resource_id + '.md' or path.name in filenames):
            raise ValueError(f'{resource_id}: 文件路径越界、格式错误或上传文件名重复')
        content = path.read_bytes()
        if not content.decode('utf-8').strip() or hashlib.sha256(content).hexdigest() != checksum:
            raise ValueError(f'{resource_id}: 文档为空或 SHA-256 校验失败')
        ids.add(resource_id)
        filenames.add(path.name)
        resources.append((record, path))
    return manifest, resources


def validate_api_url(value):
    parsed = urllib.parse.urlsplit(value)
    if (parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username is not None
            or parsed.password is not None or parsed.query or parsed.fragment):
        raise ValueError('后端地址须为 HTTP(S) 基础地址，可带应用路径；不能包含凭证、查询参数或片段')
    # Accessing port also validates malformed port strings.
    parsed.port
    return value.rstrip('/')


def package_resources(directory, resources, output):
    output = output.resolve()
    if output.is_relative_to(directory.resolve()):
        raise ValueError('离线包输出路径不能位于知识资源目录内')
    output.parent.mkdir(parents=True, exist_ok=True)
    metadata = [directory / 'manifest.json', directory / 'README.md']
    coverage = directory / 'SOURCE-COVERAGE.md'
    if coverage.is_file():
        metadata.append(coverage)
    with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED) as archive:
        for path in [*metadata, *[path for _, path in resources]]:
            archive.write(path, 'knowledge/omr/' + path.relative_to(directory.resolve()).as_posix())
        for name in ['import_knowledge.py', 'upload_docs.py']:
            archive.write(ROOT / 'scripts' / name, 'scripts/' + name)
    print(f'离线包已生成: {output}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--resource-dir', type=Path, default=RESOURCE_ROOT)
    parser.add_argument('--api-url', default=os.environ.get('API_BASE_URL', 'http://127.0.0.1:9900'))
    parser.add_argument('--case', '--id', dest='case', action='append', default=[], help='只导入指定资源编号，可多次传入；--case 兼容原案例命令')
    parser.add_argument('--kind', choices=['all', 'cases', 'references'], default='all', help='选择全部、仅案例或仅基础参考资料')
    parser.add_argument('--timeout', type=float, default=300, help='每个文件的 HTTP 超时秒数')
    parser.add_argument('--report', type=Path, help='将逐文件结果保存为 JSON，输出不包含文档正文')
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--dry-run', action='store_true', help='校验并预览，不发送请求')
    mode.add_argument('--package', type=Path, help='生成包含文档和导入脚本的独立 ZIP，不发送请求')
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error('--timeout 必须大于 0')
    directory = args.resource_dir.resolve()
    manifest, resources = load_resources(directory)
    if args.package:
        if args.case or args.report or args.kind != 'all':
            parser.error('--package 不能与资源筛选或 --report 同时使用')
        package_resources(directory, resources, args.package)
        return 0
    if args.kind != 'all':
        kind = 'case' if args.kind == 'cases' else 'reference'
        resources = [(record, path) for record, path in resources if record['kind'] == kind]
    if args.case:
        requested = set(args.case)
        known = {record['id'] for record, _ in resources}
        if requested - known:
            raise ValueError('所选资源范围中不存在编号: ' + ', '.join(sorted(requested - known)))
        resources = [(record, path) for record, path in resources if record['id'] in requested]
    if not resources:
        raise ValueError('所选范围内没有可导入的知识资源')
    api_url = validate_api_url(args.api_url)
    results = []
    for record, path in resources:
        result = {'id': record['id'], 'file': record['file'], 'kind': record['kind'],
                  'status': 'validated' if args.dry_run else 'success'}
        if args.dry_run:
            print(f'待导入: {record["id"]} {record["title"]}')
        else:
            try:
                upload(path, api_url, args.timeout, expected_sha256=record['sha256'])
                print(f'成功保存并建立索引: {record["id"]}')
            except (ValueError, OSError) as error:
                result.update(status='failed', error=str(error))
                print(f'失败: {record["id"]}: {error}', file=sys.stderr)
        results.append(result)
    failed = [result['id'] for result in results if result['status'] == 'failed']
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps({'resourceId': manifest.get('id'), 'sourceUpdated': manifest.get('sourceUpdated'),
                                          'apiUrl': api_url, 'dryRun': args.dry_run,
                                          'time': datetime.now(timezone.utc).isoformat(),
                                          'results': results, 'failed': failed}, ensure_ascii=False, indent=2) + '\n',
                               encoding='utf-8')
    print(f'{"校验待导入" if args.dry_run else "处理"} {len(results)} 份文档，失败 {len(failed)} 份')
    if failed:
        print('失败资源可通过 --id 单独重试: ' + ', '.join(failed), file=sys.stderr)
    return 1 if failed else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, OSError, KeyError) as error:
        print(f'Error: {error}', file=sys.stderr)
        sys.exit(1)
