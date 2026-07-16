#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
compose="docker compose --env-file .env.production -f docker-compose.prod.yml"

$compose exec -T app sh -c '
  tmp="$(mktemp)"
  trap '\''rm -f "$tmp"'\'' EXIT
  code="$(curl -sS -o "$tmp" -w "%{http_code}" -u "$TOSS_SECRET_KEY:" https://api.tosspayments.com/v1/payments/nonexistent-jinmifood-key-rotation-check)" # gitleaks:allow
  if [ "$code" = "404" ] && grep -q "NOT_FOUND_PAYMENT" "$tmp"; then
    echo "toss-api-auth=valid"
    exit 0
  fi
  echo "toss-api-auth=failed-status-$code"
  exit 1
'
