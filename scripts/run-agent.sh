#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"
export ONCALL_ENV_FILE="${ONCALL_ENV_FILE:-$project_root/.env}"
if [[ ! -f "$ONCALL_ENV_FILE" ]]; then
  echo "Missing Agent environment file. Copy .env.example to .env and fill in the values." >&2
  exit 1
fi
# Load this project's trusted environment without printing its values.
set -a
source "$ONCALL_ENV_FILE"
set +a
: "${DASHSCOPE_API_KEY:?Set DASHSCOPE_API_KEY in the Agent environment file}"
if [[ "${1:-}" == "--check-env" ]]; then
  exit 0
fi
# Cloud credentials are consumed by the independently started MCP server.
unset ALIBABA_CLOUD_ACCESS_KEY_ID ALIBABA_CLOUD_ACCESS_KEY_SECRET
# Use the packaged application when requested; forward remaining Spring Boot arguments.
if [[ "${1:-}" == "--jar" ]]; then
  shift
  jar_path=""
  if [[ "${1:-}" == *.jar ]]; then
    jar_path="$1"
    shift
  else
    shopt -s nullglob
    jars=("$project_root"/target/OnCallAgent-*.jar)
    if [[ ${#jars[@]} -ne 1 ]]; then
      echo "Run make package first, or pass an explicit JAR path after --jar." >&2
      exit 1
    fi
    jar_path="${jars[0]}"
  fi
  exec java -jar "$jar_path" "$@"
fi
# Cloud connections are selected in oncall.logs; no cloud-specific Spring profile is required.
exec mvn spring-boot:run "$@"
