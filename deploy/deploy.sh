#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

if [ ! -f .env.production ]; then
  echo ".env.production 파일이 없습니다. deploy/init-secrets.sh를 먼저 실행하세요."
  exit 1
fi

if [ ! -s secrets/cloudflare-tunnel-token.txt ]; then
  echo "Cloudflare Tunnel 토큰 파일이 없습니다. deploy/init-secrets.sh를 먼저 실행하세요."
  exit 1
fi

chmod 600 .env.production secrets/cloudflare-tunnel-token.txt
docker compose --env-file .env.production -f docker-compose.prod.yml config --quiet
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build --remove-orphans
docker compose --env-file .env.production -f docker-compose.prod.yml ps
