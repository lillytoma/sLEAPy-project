#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
ETL_DIR="$BACKEND_DIR/etl"
FRONTEND_DIR="$ROOT_DIR/frontend"

service_pids=()

cleanup() {
  echo
  echo "Stopping sLEAPy services..."
  for pid in "${service_pids[@]:-}"; do
    kill "$pid" 2>/dev/null || true
  done
}

trap cleanup EXIT INT TERM

start_service() {
  local name="$1"
  local directory="$2"
  local command="$3"

  (
    cd "$directory"
    stdbuf -oL -eL bash -c "$command" 2>&1
  ) > >(
    awk -v prefix="[$name] " '{ print prefix $0; fflush() }'
  ) &

  service_pids+=("$!")
  echo "$name started in the background (PID ${service_pids[-1]})."
}

start_quiet_service() {
  local name="$1"
  local directory="$2"
  local command="$3"

  (
    cd "$directory"
    bash -c "$command" >/dev/null 2>&1
  ) &

  service_pids+=("$!")
  echo "$name started in the background (PID ${service_pids[-1]}). Output is hidden."
}

wait_for_port() {
  local name="$1"
  local port="$2"
  local pid="$3"
  local attempts=0

  while (( attempts < 30 )); do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "$name failed before opening port $port. See the output above."
      return 1
    fi

    if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
      exec 3>&-
      echo "$name is running at http://localhost:$port"
      return 0
    fi

    attempts=$((attempts + 1))
    sleep 1
  done

  echo "$name did not open port $port within 30 seconds. See the output above."
  return 1
}

if [[ ! -d "$BACKEND_DIR/target" ]]; then
  start_service "backend-build" "$BACKEND_DIR" \
    "./mvnw clean install"
  build_pid="${service_pids[-1]}"
  if ! wait "$build_pid"; then
    echo "Backend build failed. The services were not started."
    exit 1
  fi
  echo "Backend build completed."
else
  echo "backend/target exists; skipping mvn clean install."
fi

if [[ ! -f "$BACKEND_DIR/target/project-0.0.1-SNAPSHOT.jar" ]]; then
  echo "The backend jar is missing. Run ./mvnw clean install."
  exit 1
fi

start_quiet_service "yfinance" "$ETL_DIR" \
  "exec python3 -m uvicorn price_service:app --host 0.0.0.0 --port 8000"
yfinance_pid="${service_pids[-1]}"

start_service "backend" "$BACKEND_DIR" \
  "if [[ -f ../.env ]]; then set -a; source ../.env; set +a; fi; exec java -jar target/project-0.0.1-SNAPSHOT.jar"
backend_pid="${service_pids[-1]}"

start_service "frontend" "$FRONTEND_DIR" \
  "exec npm run start -- --host localhost --port 4200"
frontend_pid="${service_pids[-1]}"

echo
echo "Waiting for services..."
wait_for_port "yfinance" 8000 "$yfinance_pid"
wait_for_port "backend" 8081 "$backend_pid"
wait_for_port "frontend" 4200 "$frontend_pid"

echo
echo "sLEAPy services are running:"
printf '  Frontend: \033]8;;http://localhost:4200/login\ahttp://localhost:4200/login\033]8;;\a\n'
echo "  Backend:  http://localhost:8081"
echo "  yfinance: http://localhost:8000"
echo
echo "Live service output follows below. Backend errors and stack traces will be prefixed with [backend]."
echo "Press Ctrl+C to stop all services."

wait
