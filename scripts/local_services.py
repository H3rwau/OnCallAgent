#!/usr/bin/env python3
"""Manage project-owned local services and verify deployment readiness."""
import argparse
import fcntl
import ipaddress
import json
import os
from pathlib import Path
import signal
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.run'
SETTING_KEYS = ('SERVER_PORT', 'API_BASE_URL', 'LOCAL_MCP_URL', 'ALIYUN_SLS_MCP_URL',
                'ALIYUN_SLS_MCP_SSE_ENDPOINT', 'ALIYUN_SLS_MCP_ENABLED', 'LOGS_MOCK_ENABLED',
                'WEB_PORT', 'WEB_BASE_URL', 'WEB_BIND_ADDRESS')


def settings():
    """Read only non-secret settings from the same trusted .env as the launchers."""
    values = {}
    path = Path(os.environ.get('ONCALL_ENV_FILE', str(ROOT / '.env'))).resolve()
    if path.is_file():
        code = 'import os,json,sys; print(json.dumps({k:os.environ[k] for k in sys.argv[1:] if k in os.environ}))'
        result = subprocess.run(['bash', '-c', 'set -ea\nsource "$1"\nexec "$2" -c "$3" "${@:4}"',
                                 'oncall-settings', str(path), sys.executable, code, *SETTING_KEYS],
                                capture_output=True, text=True)
        if result.returncode:
            raise RuntimeError('Cannot load project environment file')
        values.update(json.loads(result.stdout))
    # Explicit command-line/environment overrides take precedence for deployment settings.
    values.update({key: os.environ[key] for key in SETTING_KEYS if os.environ.get(key)})
    return values


def endpoints(values):
    api = values.get('API_BASE_URL') or 'http://127.0.0.1:' + values.get('SERVER_PORT', '9900')
    mcp = values.get('LOCAL_MCP_URL') or values.get('ALIYUN_SLS_MCP_URL', 'http://127.0.0.1:8181')
    if not urllib.parse.urlparse(mcp).path.strip('/'):
        mcp = mcp.rstrip('/') + values.get('ALIYUN_SLS_MCP_SSE_ENDPOINT', '/sse')
    web = values.get('WEB_BASE_URL') or 'http://127.0.0.1:' + values.get('WEB_PORT', '8080')
    return api.rstrip('/'), mcp, web.rstrip('/')


def identity(pid):
    # ps localizes lstart; a different terminal locale or TZ must not change ownership.
    result = subprocess.run(['ps', '-p', str(pid), '-o', 'stat=', '-o', 'lstart='],
                            env={**os.environ, 'LANG': 'C', 'LC_ALL': 'C', 'TZ': 'UTC'},
                            capture_output=True, text=True)
    fields = result.stdout.strip().split(None, 1)
    if result.returncode != 0 or len(fields) != 2 or fields[0].startswith('Z'):
        return None
    return fields[1]


def owned_process(record):
    try:
        return (isinstance(record['pid'], int) and bool(record['started_at'])
                and identity(record['pid']) == record['started_at']
                and os.getpgid(record['pid']) == record['pid'])
    except (KeyError, ProcessLookupError):
        return False


def ready(service, api_url, mcp_url, web_url='http://127.0.0.1:8080'):
    try:
        if service == 'mcp':
            request = urllib.request.Request(mcp_url, headers={'Accept': 'text/event-stream'})
            with urllib.request.urlopen(request, timeout=2) as response:
                return response.status == 200 and 'text/event-stream' in response.headers.get('Content-Type', '')
        if service == 'web':
            with urllib.request.urlopen(web_url + '/', timeout=2) as response:
                if response.status != 200 or b'id="app"' not in response.read(65536):
                    return False
            with urllib.request.urlopen(web_url + '/api/chat/session/deploy-readiness', timeout=2) as response:
                return response.status == 200 and json.load(response).get('message') == '会话不存在'
        with urllib.request.urlopen(api_url.rstrip('/') + '/milvus/health', timeout=2) as response:
            return response.status == 200 and json.load(response).get('message') == 'ok'
    except (OSError, ValueError, urllib.error.URLError):
        return False


def read_record(path):
    try:
        record = json.loads(path.read_text())
        if not isinstance(record, dict):
            raise ValueError('expected an object')
        return record
    except FileNotFoundError:
        return None
    except ValueError as error:
        raise RuntimeError(f'Invalid process record: {path}') from error


def local_port(url):
    parsed = urllib.parse.urlparse(url)
    if parsed.hostname is None:
        raise RuntimeError('Local service endpoint has no host')
    try:
        local = parsed.hostname == 'localhost' or ipaddress.ip_address(parsed.hostname).is_loopback
    except ValueError:
        local = False
    if not local or parsed.scheme != 'http':
        raise RuntimeError('Local service startup requires a local HTTP endpoint')
    return parsed.hostname, parsed.port or 80


def port_busy(url):
    host, port = local_port(url)
    try:
        with socket.create_connection((host, port), timeout=1):
            return True
    except OSError:
        return False


def preflight(service, api_url, mcp_url, web_url='http://127.0.0.1:8080'):
    record = read_record(STATE / f'{service}.json')
    url = {'agent': api_url, 'mcp': mcp_url, 'web': web_url}[service]
    local_port(url)
    if record and owned_process(record) and record.get('url', url) == url:
        return
    if port_busy(url):
        raise RuntimeError(f'{service} port is occupied by an external process; stop it before deploying. No process was stopped.')


def wait_ready(service, api_url, mcp_url, web_url, timeout):
    deadline = time.monotonic() + timeout
    while True:
        if service in ('agent', 'mcp', 'web'):
            record = read_record(STATE / f'{service}.json')
            if record and not owned_process(record):
                raise RuntimeError(f'{service} exited during startup; inspect {STATE / (service + ".log")}')
        if ready(service, api_url, mcp_url, web_url):
            print(f'{service}: ready')
            return
        if time.monotonic() >= deadline:
            raise RuntimeError(f'{service} readiness timed out; inspect its terminal or .run log')
        time.sleep(0.3)


def start(service, api_url, mcp_url, mode='jar', web_url='http://127.0.0.1:8080', bind_address='0.0.0.0'):
    url = {'agent': api_url, 'mcp': mcp_url, 'web': web_url}[service]
    if service == 'mcp' and ready(service, api_url, mcp_url):
        record_path = STATE / 'mcp.json'
        record = read_record(record_path)
        if record and not owned_process(record):
            record_path.unlink(missing_ok=True)
        print('mcp: existing SSE service retained')
        return
    preflight(service, api_url, mcp_url, web_url)
    record_path = STATE / f'{service}.json'
    record = read_record(record_path)
    if record and owned_process(record):
        if (record.get('mode') != mode or record.get('url') != url
                or (service == 'web' and record.get('backend_url') != api_url)):
            raise RuntimeError(f'{service} is running with different settings; stop it before starting')
        print(f'{service}: owned process already running')
        return
    commands = {
        'agent': ['bash', str(ROOT / 'scripts/run-agent.sh')],
        'mcp': [sys.executable, str(ROOT / 'scripts/aliyun_sls_mcp.py')],
    }
    if service == 'agent':
        port = str(local_port(api_url)[1])
        if mode == 'jar':
            commands['agent'] += ['--jar', '--server.port=' + port]
        else:
            commands['agent'] += ['-Dspring-boot.run.arguments=--server.port=' + port]
    child_environment = os.environ.copy()
    if service == 'web':
        if mode != 'dev' and not (ROOT / 'frontend/dist/index.html').is_file():
            raise RuntimeError('Frontend build is missing; run make frontend-build first')
        commands['web'] = ['npm', '--prefix', str(ROOT / 'frontend'), 'run',
                           'dev' if mode == 'dev' else 'preview', '--', '--host', bind_address,
                           '--port', str(local_port(web_url)[1]), '--strictPort']
        child_environment['API_PROXY_TARGET'] = api_url
    logfile = STATE / f'{service}.log'
    descriptor = os.open(logfile, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
    with os.fdopen(descriptor, 'ab') as output:
        process = subprocess.Popen(commands[service], cwd=ROOT, stdin=subprocess.DEVNULL,
                                   stdout=output, stderr=subprocess.STDOUT, start_new_session=True,
                                   env=child_environment)
    started_at = identity(process.pid)
    if not started_at:
        raise RuntimeError(f'{service} exited during startup; inspect {logfile}')
    record_path.write_text(json.dumps({'pid': process.pid, 'started_at': started_at, 'mode': mode,
                                       'url': url, 'backend_url': api_url}))
    record_path.chmod(0o600)
    print(f'{service}: started (PID {process.pid}); log: {logfile}')


def stop(service):
    record_path = STATE / f'{service}.json'
    record = read_record(record_path)
    if not record:
        print(f'{service}: no management record; no signal sent to IDE or external processes')
        return
    if not owned_process(record):
        pid = record.get('pid')
        if isinstance(pid, int) and identity(pid) is not None:
            raise RuntimeError(f'{service}: process is alive but ownership cannot be verified; record retained, no signal sent')
        record_path.unlink(missing_ok=True)
        print(f'{service}: recorded process has exited; external processes retained')
        return
    os.killpg(record['pid'], signal.SIGTERM)
    deadline = time.monotonic() + 15
    while owned_process(record):
        if time.monotonic() > deadline:
            raise RuntimeError(f'{service} has not exited; inspect {STATE / (service + ".log")}')
        time.sleep(0.2)
    record_path.unlink(missing_ok=True)
    print(f'{service}: stopped')


def main():
    values = settings()
    api, mcp, web = endpoints(values)
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['start', 'stop', 'restart', 'check', 'wait', 'status', 'preflight', 'ensure', 'config'])
    parser.add_argument('service', choices=['agent', 'mcp', 'web', 'backend-port', 'api-url'])
    parser.add_argument('--api-url', default=api)
    parser.add_argument('--mcp-url', default=mcp)
    parser.add_argument('--web-url', default=web)
    parser.add_argument('--timeout', type=float, default=180)
    parser.add_argument('--mode', choices=['jar', 'dev', 'preview'])
    args = parser.parse_args()
    api, mcp, web = args.api_url, args.mcp_url, args.web_url
    if args.action == 'config':
        if args.service == 'backend-port':
            print(local_port(api)[1])
        elif args.service == 'api-url':
            parsed = urllib.parse.urlsplit(api)
            if (parsed.scheme not in ('http', 'https') or not parsed.hostname or parsed.username is not None
                    or parsed.password is not None or parsed.query or parsed.fragment):
                raise RuntimeError('API endpoint must be an HTTP(S) base URL without credentials or query parameters')
            parsed.port
            print(api.rstrip('/'))
        else:
            parser.error('config supports backend-port and api-url only')
        return 0
    if args.service in ('backend-port', 'api-url'):
        parser.error('this setting is only valid with config')
    if args.action in ['check', 'status']:
        healthy = ready(args.service, api, mcp, web)
        print(f'{args.service}: {"ready" if healthy else "not ready"}')
        return 0 if healthy or args.action == 'status' else 1
    if args.action == 'wait':
        wait_ready(args.service, api, mcp, web, args.timeout)
        return 0
    mode = args.mode or ('preview' if args.service == 'web' else 'jar')
    if args.action == 'ensure' and args.service != 'mcp':
        parser.error('ensure supports mcp only')
    if args.action == 'ensure':
        if values.get('LOGS_MOCK_ENABLED', '').lower() == 'true' or values.get('ALIYUN_SLS_MCP_ENABLED', 'true').lower() == 'false':
            print('mcp: local SLS startup skipped by project configuration')
            return 0
        try:
            local_port(mcp)
        except RuntimeError:
            if not ready('mcp', api, mcp):
                raise RuntimeError('Configured remote SLS SSE service is not ready; no local service was started')
            print('mcp: remote SSE service ready')
            return 0
    if args.action == 'preflight':
        preflight(args.service, api, mcp, web)
        return 0
    STATE.mkdir(mode=0o700, exist_ok=True)
    STATE.chmod(0o700)
    with (STATE / f'{args.service}.lock').open('a') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        if args.action == 'stop':
            stop(args.service)
            return 0
        if args.action == 'restart':
            preflight(args.service, api, mcp, web)
            stop(args.service)
        start(args.service, api, mcp, mode, web, values.get('WEB_BIND_ADDRESS', '0.0.0.0'))
        try:
            wait_ready(args.service, api, mcp, web, args.timeout)
        except RuntimeError:
            stop(args.service)
            raise
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, OSError, ValueError) as error:
        print(f'Error: {error}', file=sys.stderr)
        sys.exit(1)
