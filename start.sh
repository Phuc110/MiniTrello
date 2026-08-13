#!/usr/bin/env bash
set -euo pipefail

# One-command local bring-up for the whole stack (mysql + redis + backend + frontend).
# Usage: ./start.sh

cd "$(dirname "$0")"

if [ ! -f backend/.env ]; then
  echo "==> No backend/.env found — creating one from backend/.env.example"
  cp backend/.env.example backend/.env

  # Auto-generate a real JWT secret so this works out of the box instead
  # of booting with the placeholder value (which Spring will accept but
  # which is not safe to leave in place even for local dev).
  if command -v openssl >/dev/null 2>&1; then
    JWT_SECRET=$(openssl rand -base64 64 | tr -d '\n')
    if [[ "$OSTYPE" == "darwin"* ]]; then
      sed -i '' "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" backend/.env
    else
      sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" backend/.env
    fi
    echo "==> Generated a random JWT_SECRET in backend/.env"
  else
    echo "==> openssl not found — edit backend/.env and set JWT_SECRET manually before continuing"
    exit 1
  fi
fi

echo "==> Building and starting all services (this can take a few minutes on first run)"
docker compose up --build -d

echo "==> Waiting for the backend to report healthy..."
for _ in $(seq 1 30); do
  status=$(docker compose ps --format json backend 2>/dev/null | grep -o '"Health":"[a-z]*"' | cut -d'"' -f4 || echo "")
  if [ "$status" = "healthy" ]; then
    echo "==> Backend is healthy."
    break
  fi
  sleep 5
done

cat <<'EOF'

==> Stack is up.

  Frontend:    http://localhost
  Backend API: http://localhost:8080
  Swagger UI:  http://localhost:8080/swagger-ui.html

  View logs:   docker compose logs -f
  Stop:        docker compose down
  Stop + wipe: docker compose down -v   (also deletes the database volume)

EOF
