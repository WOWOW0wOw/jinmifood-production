#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."

docker compose --env-file .env.production -f docker-compose.prod.yml exec -T db sh -c '
  psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -F= <<"SQL"
select $$members$$,count(*) from members;
select $$social_accounts$$,count(*) from social_accounts;
select $$linked_members$$,count(distinct member_id) from social_accounts;
select $$orphan_social_accounts$$,count(*) from social_accounts s left join members m on m.id=s.member_id where m.id is null;
select provider,count(*) from social_accounts group by provider order by provider;
select $$social_created_last_24h$$,count(*) from social_accounts where created_at >= now() - make_interval(hours => 24);
SQL
'

docker compose --env-file .env.production -f docker-compose.prod.yml logs --since=3h app 2>&1 \
  | grep -E 'OAuth2|oauth2|account_disabled|authorization_request_not_found|invalid_client|redirect_uri|ERROR|Exception' \
  | tail -n 120 || true
