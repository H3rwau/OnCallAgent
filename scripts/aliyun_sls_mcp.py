#!/usr/bin/env python3
"""Launch the official SLS MCP server with credentials read at runtime."""

import argparse
import os
from pathlib import Path
import platform
import shlex
import sys
import tarfile
import tempfile
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
VERSION = "v2.0.7"
BINARY = ROOT / ".tools" / VERSION / "alibabacloud-observability-mcp-server"


def install():
    system = {"Darwin": "darwin", "Linux": "linux"}.get(platform.system())
    machine = {"arm64": "arm64", "aarch64": "arm64", "x86_64": "amd64"}.get(platform.machine())
    if not system or not machine:
        raise ValueError("Automatic installation supports macOS/Linux arm64 and amd64 only")
    asset = f"alibabacloud-observability-mcp-server-{system}-{machine}.tar.gz"
    url = f"https://github.com/aliyun/alibabacloud-observability-mcp-server/releases/download/{VERSION}/{asset}"
    with tempfile.TemporaryDirectory(prefix="oncall-sls-mcp-") as directory:
        archive = Path(directory) / asset
        urllib.request.urlretrieve(url, archive)
        with tarfile.open(archive, "r:gz") as tar:
            members = [m for m in tar.getmembers()
                       if m.isfile() and Path(m.name).name == BINARY.name]
            if len(members) != 1:
                raise ValueError("Release archive does not contain a unique MCP executable")
            with tar.extractfile(members[0]) as source:
                binary_data = source.read()
        BINARY.parent.mkdir(parents=True, exist_ok=True)
        BINARY.write_bytes(binary_data)
        BINARY.chmod(0o755)
    print(f"Installed official SLS MCP server {VERSION}: {BINARY}")


def server_environment(env_path):
    values = {}
    for raw_line in env_path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[7:].lstrip()
        key, separator, value = line.partition("=")
        key = key.strip()
        if key not in ("ALIBABA_CLOUD_ACCESS_KEY_ID", "ALIBABA_CLOUD_ACCESS_KEY_SECRET"):
            continue
        if not separator:
            raise ValueError("Invalid SLS environment entry")
        parts = shlex.split(value, comments=True)
        if len(parts) != 1 or not parts[0].strip():
            raise ValueError("Missing or invalid SLS credential in project environment file")
        values[key] = parts[0]
    required = ("ALIBABA_CLOUD_ACCESS_KEY_ID", "ALIBABA_CLOUD_ACCESS_KEY_SECRET")
    if any(not values.get(key) for key in required):
        raise ValueError("Configure both SLS credentials in the project environment file")
    env = os.environ.copy()
    env.update(values)
    return env


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--install", action="store_true", help="Install the pinned official release")
    parser.add_argument("--env-file", type=Path,
                        default=Path(os.environ.get("ONCALL_ENV_FILE", str(ROOT / ".env"))))
    parser.add_argument("--binary", type=Path,
                        default=Path(os.environ.get("ALIYUN_SLS_MCP_BINARY", str(BINARY))))
    args = parser.parse_args()
    if args.install:
        install()
        return
    binary = args.binary.expanduser().resolve()
    if not binary.is_file() or not os.access(binary, os.X_OK):
        raise ValueError("MCP executable missing; run: python3 scripts/aliyun_sls_mcp.py --install")
    env = server_environment(args.env_file.expanduser())
    command = [str(binary), "start", "--config", str(ROOT / "config" / "aliyun-sls-mcp.yaml")]
    # Run in the foreground so the operator controls the independent SSE service.
    os.execve(str(binary), command, env)


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, tarfile.TarError, urllib.error.URLError):
        # Configuration parsing and HTTP errors can contain secrets; never print their raw text.
        print("SLS MCP startup failed. Check the executable, project .env file, and network; "
              "install with: python3 scripts/aliyun_sls_mcp.py --install", file=sys.stderr)
        sys.exit(1)
