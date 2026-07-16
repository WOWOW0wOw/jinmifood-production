#!/usr/bin/env sh
set -eu

if [ -z "${ADMIN_USERNAME:-}" ] || [ -z "${ADMIN_PASSWORD:-}" ] || [ -z "${APP_PUBLIC_BASE_URL:-}" ]; then
  compose="docker compose --env-file .env.production -f docker-compose.prod.yml"
  ADMIN_USERNAME="${ADMIN_USERNAME:-$($compose exec -T app printenv ADMIN_USERNAME)}"
  ADMIN_PASSWORD="${ADMIN_PASSWORD:-$($compose exec -T app printenv ADMIN_PASSWORD)}"
  APP_PUBLIC_BASE_URL="${APP_PUBLIC_BASE_URL:-$($compose exec -T app printenv APP_PUBLIC_BASE_URL)}"
fi

base_url="${BASE_URL:-${APP_PUBLIC_BASE_URL:-https://jinmifood.com}}"
base_url="${base_url%/}"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

curl -fsS -c "$work_dir/cookies" "$base_url/login" > "$work_dir/login.html"
csrf="$(sed -n 's/.*name="_csrf"[^>]*value="\([^"]*\)".*/\1/p' "$work_dir/login.html" | head -n 1)"
if [ -z "$csrf" ]; then
  echo "admin-login-csrf=missing"
  exit 1
fi

login_status="$(curl -sS -D "$work_dir/login-headers" -o "$work_dir/login-result.html" -w '%{http_code}' -b "$work_dir/cookies" -c "$work_dir/cookies" \
  -X POST "$base_url/login" \
  --data-urlencode "username=$ADMIN_USERNAME" \
  --data-urlencode "password=$ADMIN_PASSWORD" \
  --data-urlencode "_csrf=$csrf")"
echo "admin-login-status=$login_status"
login_location="$(sed -n 's/^location: //Ip' "$work_dir/login-headers" | tr -d '\r' | tail -n 1)"
echo "admin-login-location=$login_location"
if [ "$login_status" != "302" ]; then
  echo "admin-login-status=$login_status"
  exit 1
fi
if [ "$login_location" != "$base_url/" ] && [ "$login_location" != "/" ]; then
  echo "admin-login=failed"
  exit 1
fi

members_status="$(curl -sS -o "$work_dir/members.html" -w '%{http_code}' -b "$work_dir/cookies" "$base_url/admin/members")"
echo "member-list-status=$members_status"
grep -Eq '20[0-9]{2}-[0-9]{2}-[0-9]{2}' "$work_dir/members.html"
grep -q 'GOOGLE' "$work_dir/members.html"
grep -q 'KAKAO' "$work_dir/members.html"
grep -q 'NAVER' "$work_dir/members.html"
grep -q '회원 관리' "$work_dir/members.html"
grep -q '상세·관리' "$work_dir/members.html"

detail_path="$(sed -n 's/.*href="\(\/admin\/members\/[0-9][0-9]*\)".*/\1/p' "$work_dir/members.html" | head -n 1)"
if [ -z "$detail_path" ]; then
  echo "member-detail-link=missing"
  exit 1
fi
detail_status="$(curl -sS -o "$work_dir/detail.html" -w '%{http_code}' -b "$work_dir/cookies" "$base_url$detail_path")"
echo "member-detail-status=$detail_status"
orders_status="$(curl -sS -o "$work_dir/orders.html" -w '%{http_code}' -b "$work_dir/cookies" "$base_url/admin/orders")"
echo "order-list-status=$orders_status"
grep -Eq '20[0-9]{2}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}' "$work_dir/orders.html"
grep -q '회원 정보' "$work_dir/detail.html"
grep -q '포인트 조정' "$work_dir/detail.html"
grep -q '변경 이력' "$work_dir/detail.html"

echo "member-management-markers=ok"
echo "social-provider-badges=ok"
echo "korea-datetime-markers=ok"
