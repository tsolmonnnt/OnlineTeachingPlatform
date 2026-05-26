#!/bin/sh
set -eu

DOMAIN="${1:-onlineteaching.dev}"
EMAIL="${2:-}"

if [ -z "$EMAIL" ]; then
  echo "Usage: sh infra/certbot/init-letsencrypt.sh <domain> <email>"
  exit 1
fi

echo "Starting HTTP-only stack for initial certificate issuance..."
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d db backend frontend nginx

echo "Requesting Let's Encrypt certificate for $DOMAIN ..."
docker compose --env-file .env.prod -f docker-compose.prod.yml run --rm certbot \
  certonly \
  --webroot \
  -w /var/www/certbot \
  -d "$DOMAIN" \
  --email "$EMAIL" \
  --agree-tos \
  --no-eff-email

echo "Certificate obtained."
echo "Now update .env.prod:"
echo "  NGINX_CONF_FILE=prod.conf"
echo "Then run:"
echo "  docker compose --env-file .env.prod -f docker-compose.prod.yml up -d"
