#!/usr/bin/env bash
set -euo pipefail

SERVER_PORT="${SERVER_PORT:-19100}"
AUTH_MODE="${AUTH_MODE:-local}"
SEED_LARGE="false"
SEED_USER_ID="${SEED_USER_ID:-admin}"
SEED_PASSWORD="${SEED_PASSWORD:-admin1234}"

print_usage() {
  cat <<'EOF'
Usage: ./run-local.sh [options]

Options:
  -p, --server-port <port>  Backend port (default: 19100)
  -a, --auth-mode <mode>    Auth mode: local | mock (default: local)
  -s, --seed-large          Start large seed automatically after backend is ready
      --seed-user <id>      Seed API login userId (default: admin)
      --seed-password <pw>  Seed API login password (default: admin1234)
  -h, --help                Show this help

Examples:
  ./run-local.sh
  ./run-local.sh --server-port 19100 --auth-mode local
  ./run-local.sh --seed-large
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|--server-port)
      if [[ $# -lt 2 ]]; then
        echo "[local] missing value for $1" >&2
        exit 1
      fi
      SERVER_PORT="$2"
      shift 2
      ;;
    -a|--auth-mode)
      if [[ $# -lt 2 ]]; then
        echo "[local] missing value for $1" >&2
        exit 1
      fi
      AUTH_MODE="$2"
      shift 2
      ;;
    -s|--seed-large)
      SEED_LARGE="true"
      shift
      ;;
    --seed-user)
      if [[ $# -lt 2 ]]; then
        echo "[local] missing value for $1" >&2
        exit 1
      fi
      SEED_USER_ID="$2"
      shift 2
      ;;
    --seed-password)
      if [[ $# -lt 2 ]]; then
        echo "[local] missing value for $1" >&2
        exit 1
      fi
      SEED_PASSWORD="$2"
      shift 2
      ;;
    -h|--help)
      print_usage
      exit 0
      ;;
    *)
      echo "[local] unknown option: $1" >&2
      print_usage
      exit 1
      ;;
  esac
done

if [[ "$AUTH_MODE" != "local" && "$AUTH_MODE" != "mock" ]]; then
  echo "[local] invalid auth mode: $AUTH_MODE (use: local|mock)" >&2
  exit 1
fi

if command -v pwsh >/dev/null 2>&1; then
  PS_EXE="pwsh"
elif command -v powershell.exe >/dev/null 2>&1; then
  PS_EXE="powershell.exe"
elif command -v powershell >/dev/null 2>&1; then
  PS_EXE="powershell"
else
  echo "[local] PowerShell executable not found (pwsh/powershell)." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "[local] npm command not found." >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
BACKEND_BASE_URL="http://localhost:$SERVER_PORT"
BACKEND_STARTED="false"

if [[ ! -f "$BACKEND_DIR/run-gradle.ps1" ]]; then
  echo "[local] backend/run-gradle.ps1 not found." >&2
  exit 1
fi

cleanup() {
  if [[ "${BACKEND_STARTED}" == "true" ]] && [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    echo "[local] stopping backend (pid=$BACKEND_PID)..."
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

is_backend_up() {
  "$PS_EXE" -NoProfile -Command "try { \$r=Invoke-RestMethod -Uri '$BACKEND_BASE_URL/actuator/health' -Method Get -TimeoutSec 2; if (\$r.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }"
}

wait_for_backend_ready() {
  echo "[local] waiting for backend health check..."
  local max_attempts=180
  local attempt
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    if "$PS_EXE" -NoProfile -Command "try { \$r=Invoke-RestMethod -Uri '$BACKEND_BASE_URL/actuator/health' -Method Get -TimeoutSec 2; if (\$r.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }"; then
      echo "[local] backend is UP."
      return 0
    fi
    sleep 1
  done
  echo "[local] backend health check timeout." >&2
  return 1
}

trigger_large_seed() {
  echo "[local] triggering large seed..."
  if SEED_USER_ID="$SEED_USER_ID" SEED_PASSWORD="$SEED_PASSWORD" "$PS_EXE" -NoProfile -Command "\
    \$ErrorActionPreference='Stop'; \
    \$s=New-Object Microsoft.PowerShell.Commands.WebRequestSession; \
    \$body=@{ userId=\$env:SEED_USER_ID; password=\$env:SEED_PASSWORD } | ConvertTo-Json; \
    Invoke-RestMethod -Uri '$BACKEND_BASE_URL/api/v1/auth/login' -Method Post -ContentType 'application/json' -Body \$body -WebSession \$s | Out-Null; \
    \$resp=Invoke-RestMethod -Uri '$BACKEND_BASE_URL/api/v1/admin/seed/start' -Method Post -WebSession \$s; \
    Write-Output ('[local] seed state=' + \$resp.data.state + ', message=' + \$resp.data.message)"; then
    return 0
  fi
  echo "[local] failed to trigger large seed. check seed credentials or backend logs." >&2
  return 1
}

if is_backend_up; then
  echo "[local] backend is already running at $BACKEND_BASE_URL (skip bootRun)."
else
  echo "[local] backend start... (SERVER_PORT=$SERVER_PORT)"
  (
    cd "$BACKEND_DIR"
    SERVER_PORT="$SERVER_PORT" "$PS_EXE" -NoProfile -ExecutionPolicy Bypass -File "./run-gradle.ps1" bootRun
  ) &
  BACKEND_PID=$!
  BACKEND_STARTED="true"
fi

if [[ "$SEED_LARGE" == "true" ]]; then
  if wait_for_backend_ready; then
    trigger_large_seed || true
  fi
else
  sleep 2
fi

echo "[local] frontend start... (VITE_API_BASE_URL=http://localhost:$SERVER_PORT, VITE_AUTH_MODE=$AUTH_MODE)"
cd "$FRONTEND_DIR"
VITE_API_BASE_URL="http://localhost:$SERVER_PORT" VITE_AUTH_MODE="$AUTH_MODE" npm run dev
