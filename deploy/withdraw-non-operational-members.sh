#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
compose="docker compose --env-file .env.production -f docker-compose.prod.yml"

admin_username="$($compose exec -T app printenv ADMIN_USERNAME | tr -d '\r')"
admin_password="$($compose exec -T app printenv ADMIN_PASSWORD | tr -d '\r')"
base_url="$($compose exec -T app printenv APP_PUBLIC_BASE_URL | tr -d '\r')"
db_user="$($compose exec -T db printenv POSTGRES_USER | tr -d '\r')"
db_name="$($compose exec -T db printenv POSTGRES_DB | tr -d '\r')"

work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

curl -fsS -c "$work_dir/cookies" "$base_url/login" > "$work_dir/login.html"
csrf="$(sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p' "$work_dir/login.html" | head -n 1)"
test -n "$csrf"

login_status="$(curl -sS -o /dev/null -w '%{http_code}' -b "$work_dir/cookies" -c "$work_dir/cookies" \
  -X POST "$base_url/login" \
  --data-urlencode "username=$admin_username" \
  --data-urlencode "password=$admin_password" \
  --data-urlencode "_csrf=$csrf")"
test "$login_status" = "302"

curl -fsS -b "$work_dir/cookies" -c "$work_dir/cookies" "$base_url/admin/members" > "$work_dir/members.html"
csrf="$(sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p' "$work_dir/members.html" | head -n 1)"
test -n "$csrf"

target_ids="$(
$compose exec -T db psql -U "$db_user" -d "$db_name" -v admin_username="$admin_username" -At <<'SQL'
SELECT id FROM members
WHERE lower(email) <> lower(:'admin_username') AND withdrawn_at IS NULL
ORDER BY id;
SQL
)"

target_count=0
completed_count=0
for member_id in $target_ids; do
  target_count=$((target_count + 1))
  status="$(curl -sS -o /dev/null -w '%{http_code}' -b "$work_dir/cookies" -c "$work_dir/cookies" \
    -X POST "$base_url/admin/members/$member_id/withdraw" \
    --data-urlencode "reason=운영 관리자 요청 일괄 탈퇴" \
    --data-urlencode "_csrf=$csrf")"
  if [ "$status" != "302" ]; then
    echo "member-withdrawal-failed: id=$member_id status=$status" >&2
    exit 1
  fi
  completed_count=$((completed_count + 1))
done

remaining_non_admin="$(
$compose exec -T db psql -U "$db_user" -d "$db_name" -v admin_username="$admin_username" -At <<'SQL'
SELECT count(*) FROM members
WHERE lower(email) <> lower(:'admin_username') AND withdrawn_at IS NULL;
SQL
)"
operational_admins="$(
$compose exec -T db psql -U "$db_user" -d "$db_name" -v admin_username="$admin_username" -At <<'SQL'
SELECT count(*) FROM members
WHERE lower(email)=lower(:'admin_username') AND admin=true AND active=true AND withdrawn_at IS NULL;
SQL
)"

echo "withdrawal-targets=$target_count"
echo "withdrawal-completed=$completed_count"
echo "remaining-non-operational-members=$remaining_non_admin"
echo "operational-admins=$operational_admins"
test "$remaining_non_admin" = "0"
test "$operational_admins" = "1"
