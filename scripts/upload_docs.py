#!/usr/bin/env python3
"""Upload selected Markdown documents; require file and index success."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.request
import uuid


def document_directory(value):
    if not value.strip():
        raise argparse.ArgumentTypeError('文档目录不能为空')
    return Path(value)


def upload(path, api_url, timeout, expected_sha256=None):
    content = path.read_bytes()
    if expected_sha256 is not None and hashlib.sha256(content).hexdigest() != expected_sha256:
        raise ValueError('文档内容与资源清单校验值不一致')
    if not content.strip():
        raise ValueError('文档为空')
    boundary = 'oncall-' + uuid.uuid4().hex
    filename = path.name.replace('\\', '\\\\').replace('"', '\\"').replace('\r', '').replace('\n', '')
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\n'
            'Content-Type: text/markdown; charset=UTF-8\r\n\r\n').encode() + content
    body += f'\r\n--{boundary}--\r\n'.encode()
    request = urllib.request.Request(api_url.rstrip('/') + '/api/upload', data=body,
                                    headers={'Content-Type': 'multipart/form-data; boundary=' + boundary,
                                             'Accept': 'application/json'}, method='POST')
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            payload = json.load(response)
    except urllib.error.HTTPError as error:
        try:
            message = json.load(error).get('message', '请检查后端日志')
        except (ValueError, AttributeError):
            message = '请检查后端日志'
        raise ValueError(f'HTTP {error.code}: {message}') from error
    if not isinstance(payload, dict) or payload.get('code') != 200:
        raise ValueError('接口报告失败: ' + str(payload.get('message', '响应格式异常') if isinstance(payload, dict) else '响应格式异常'))
    data = payload.get('data')
    if not isinstance(data, dict) or data.get('fileName') != path.name or data.get('fileSize') != len(content) or not data.get('filePath'):
        raise ValueError('接口未返回完整的文件确认信息')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=document_directory, required=True, help='显式指定要上传的 Markdown 目录')
    parser.add_argument('--api-url', default=os.environ.get('API_BASE_URL', 'http://127.0.0.1:9900'))
    parser.add_argument('--timeout', type=float, default=300)
    parser.add_argument('--dry-run', action='store_true', help='只列出待导入文件，不发送请求')
    args = parser.parse_args()
    files = sorted(args.directory.glob('*.md')) if args.directory.is_dir() else []
    if not files:
        print(f'没有可导入的 Markdown 文档: {args.directory}', file=sys.stderr)
        return 1
    failed = 0
    for path in files:
        if args.dry_run:
            print(f'待导入: {path.name} ({path.stat().st_size} bytes)')
            continue
        try:
            upload(path, args.api_url, args.timeout)
            print(f'成功保存并建立索引: {path.name}')
        except (ValueError, OSError, urllib.error.URLError) as error:
            print(f'失败: {path.name}: {error}', file=sys.stderr)
            failed += 1
    print(f'{"待导入" if args.dry_run else "处理"} {len(files)} 份文档，失败 {failed} 份')
    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
