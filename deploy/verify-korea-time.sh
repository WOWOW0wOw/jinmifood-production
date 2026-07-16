#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

compose() {
  docker compose --env-file .env.production -f docker-compose.prod.yml "$@"
}

printf 'app-time='
compose exec -T app date -Iseconds
printf 'db-container-time='
compose exec -T db date -Iseconds

compose exec -T db sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At' <<'SQL'
select 'migration-v12=' || success from flyway_schema_history where version = '12';
select 'latest-member=' || to_char(max(created_at), 'YYYY-MM-DD HH24:MI:SS') from members;
select 'latest-order=' || coalesce(to_char(max(created_at), 'YYYY-MM-DD HH24:MI:SS'), 'none') from customer_orders;
SQL

curl -fsS https://jinmifood.com/actuator/health
echo
