#!/usr/bin/env bash
set -euo pipefail

SERVER_PORT="${SERVER_PORT:-19100}"
AUTH_MODE="${AUTH_MODE:-local}"

print_usage() {
  cat <<'EOF'
Usage: ./run-local.sh [options]

Options:
  -p, --server-port <port>  Backend port (default: 19100)
  -a, --auth-mode <mode>    Auth mode: local | mock (default: local)
  -h, --help                Show this help

Examples:
  ./run-local.sh
  ./run-local.sh --server-port 19100 --auth-mode local
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

if [[ ! -f "$BACKEND_DIR/run-gradle.ps1" ]]; then
  echo "[local] backend/run-gradle.ps1 not found." >&2
  exit 1
fi

cleanup() {
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" >/dev/null 2>&1; then
    echo "[local] stopping backend (pid=$BACKEND_PID)..."
    kill "$BACKEND_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

echo "[local] backend start... (SERVER_PORT=$SERVER_PORT)"
(
  cd "$BACKEND_DIR"
  SERVER_PORT="$SERVER_PORT" "$PS_EXE" -NoProfile -ExecutionPolicy Bypass -File "./run-gradle.ps1" bootRun
) &
BACKEND_PID=$!

sleep 2

echo "[local] frontend start... (VITE_API_BASE_URL=http://localhost:$SERVER_PORT, VITE_AUTH_MODE=$AUTH_MODE)"
cd "$FRONTEND_DIR"
VITE_API_BASE_URL="http://localhost:$SERVER_PORT" VITE_AUTH_MODE="$AUTH_MODE" npm run dev
