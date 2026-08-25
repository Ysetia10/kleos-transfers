#!/usr/bin/env bash
# Finish Supabase setup: CLI link + restore local Postgres dump.
# Usage:
#   export SUPABASE_ACCESS_TOKEN="<from dashboard account tokens>"
#   export SUPABASE_DB_PASSWORD="<database password you saved>"
#   ./scripts/complete-supabase.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DUMP="$ROOT/.tools/kleos.dump"

if [[ -z "${SUPABASE_ACCESS_TOKEN:-}" ]]; then
  echo "Missing SUPABASE_ACCESS_TOKEN. Create one at:"
  echo "  https://supabase.com/dashboard/account/tokens"
  exit 1
fi

if [[ -z "${SUPABASE_DB_PASSWORD:-}" ]]; then
  echo "Missing SUPABASE_DB_PASSWORD (the password you set when creating the project)."
  exit 1
fi

echo "==> Listing Supabase projects..."
npx --yes supabase@latest projects list

REF="$(npx --yes supabase@latest projects list -o json | node -e "
  const d=JSON.parse(require('fs').readFileSync(0,'utf8'));
  const p=d.find(x=>x.name==='kleos-transfers');
  if(!p){console.error('kleos-transfers not found');process.exit(1);}
  process.stdout.write(p.id);
")"

echo "==> Linking local repo (ref: $REF)..."
cd "$ROOT"
npx --yes supabase@latest link --project-ref "$REF"

if [[ -z "$REF" ]]; then
  echo "Could not find project ref for kleos-transfers"
  exit 1
fi

HOST="db.${REF}.supabase.co"
POOLER_HOST="aws-0-us-west-2.pooler.supabase.com"
# Render and other cloud hosts need the pooler (IPv4); direct db.* host is often IPv6-only.
JDBC="jdbc:postgresql://${POOLER_HOST}:5432/postgres?sslmode=require"
POOLER_USER="postgres.${REF}"

echo ""
echo "==> Connection details for Render (use pooler — NOT direct db.* host):"
echo "DATABASE_URL=$JDBC"
echo "DATABASE_USERNAME=$POOLER_USER"
echo "DATABASE_PASSWORD=(your saved password)"
echo "Direct host (local pg_restore only): $HOST"
echo ""

echo "==> Dumping local kleos_transfers..."
mkdir -p "$ROOT/.tools"
pg_dump -h localhost -U kleos -d kleos_transfers --no-owner --no-acl -Fc -f "$DUMP"

echo "==> Restoring to Supabase (this may take a few minutes)..."
PGPASSWORD="$SUPABASE_DB_PASSWORD" pg_restore \
  -h "$HOST" \
  -U postgres \
  -d postgres \
  --no-owner \
  --no-acl \
  --clean \
  --if-exists \
  "$DUMP"

echo ""
echo "==> Supabase setup complete."
echo "Use these on Render:"
echo "  DATABASE_URL=$JDBC"
echo "  DATABASE_USERNAME=$POOLER_USER"
echo "  CORS_ALLOWED_ORIGINS=https://kleos-transfer.vercel.app"
