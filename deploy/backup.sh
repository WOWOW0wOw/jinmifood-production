#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
install -d -m 700 backups
timestamp="$(date +%Y%m%d-%H%M%S)"

docker compose --env-file .env.production -f docker-compose.prod.yml exec -T db \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "backups/jinmi-$timestamp.dump"

if [ ! -s "backups/jinmi-$timestamp.dump" ]; then
  echo "DB backup validation failed: empty dump"
  exit 1
fi

docker compose --env-file .env.production -f docker-compose.prod.yml exec -T app \
  tar -C /app/uploads -czf - . > "backups/product-uploads-$timestamp.tar.gz"

find backups -type f -mtime +30 -delete

echo "백업 완료: backups/jinmi-$timestamp.dump"
echo "백업 완료: backups/product-uploads-$timestamp.tar.gz"
echo "서버 장애에 대비해 backups 폴더를 별도 저장소에도 복사하세요."
