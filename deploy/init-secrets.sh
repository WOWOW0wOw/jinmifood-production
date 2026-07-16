#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."
umask 077
mkdir -p secrets

env_file=".env.production"
tunnel_token_file="secrets/cloudflare-tunnel-token.txt"

if [[ -e "$env_file" ]]; then
  echo "Production environment file already exists: $env_file"
  echo "Back it up and remove it manually before replacing it."
  exit 1
fi

read -r -p "Admin username: " admin_username
read -r -s -p "Admin password (12+ characters): " admin_password
echo
read -r -p "Toss mode (test/live; test recommended initially): " toss_mode
read -r -s -p "Toss client key: " toss_client_key
echo
read -r -s -p "Toss secret key: " toss_secret_key
echo

if [[ ! -s "$tunnel_token_file" ]]; then
  read -r -s -p "Cloudflare Tunnel token: " tunnel_token
  echo
else
  tunnel_token=""
  echo "Using the existing Cloudflare Tunnel token file."
fi

if [[ -z "$admin_username" ]]; then
  echo "Admin username cannot be empty."
  exit 1
fi
if [[ ${#admin_password} -lt 12 ]]; then
  echo "Admin password must contain at least 12 characters."
  exit 1
fi
if [[ "$toss_mode" != "test" && "$toss_mode" != "live" ]]; then
  echo "Toss mode must be test or live."
  exit 1
fi
if [[ -z "$toss_client_key" || -z "$toss_secret_key" ]]; then
  echo "Toss keys cannot be empty."
  exit 1
fi
if [[ ! -s "$tunnel_token_file" && -z "$tunnel_token" ]]; then
  echo "Cloudflare Tunnel token cannot be empty."
  exit 1
fi

# Compose env 파일의 단일 인용 값이 깨지지 않도록 위험 문자를 거부합니다.
for value in "$admin_username" "$admin_password" "$toss_client_key" "$toss_secret_key"; do
  if [[ "$value" == *"'"* || "$value" == *$'\n'* || "$value" == *$'\r'* ]]; then
    echo "Admin values and Toss keys cannot contain a single quote or newline."
    exit 1
  fi
done

db_password="$(openssl rand -base64 36 | tr -d '\n')"

cat > "$env_file" <<EOF
DOMAIN=jinmifood.com
POSTGRES_DB=jinmi
POSTGRES_USER=jinmi
POSTGRES_PASSWORD='$db_password'
ADMIN_USERNAME='$admin_username'
ADMIN_PASSWORD='$admin_password'
TOSS_MODE=$toss_mode
TOSS_CLIENT_KEY='$toss_client_key'
TOSS_SECRET_KEY='$toss_secret_key'
TOSS_CONNECT_TIMEOUT=3s
TOSS_READ_TIMEOUT=10s
DB_POOL_SIZE=10
DATA_RETENTION_ENABLED=true
UNPAID_ORDER_TTL=7d
DATA_CLEANUP_BATCH_SIZE=100
DATA_CLEANUP_CRON=0 17 * * * *
DATA_CLEANUP_ZONE=Asia/Seoul
EOF

if [[ ! -s "$tunnel_token_file" ]]; then
  printf '%s' "$tunnel_token" > "$tunnel_token_file"
fi
chmod 600 "$env_file" "$tunnel_token_file"
unset admin_password toss_client_key toss_secret_key tunnel_token db_password value

echo "Production secrets were created without printing sensitive values."
