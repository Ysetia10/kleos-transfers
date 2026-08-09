#!/usr/bin/env bash
# After FBref ingest for new top-five leagues: crests, height/foot, DOB, photos.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
API_URL="${API_URL:-http://localhost:8080}"

echo "== Club crests =="
python3 -u scripts/enrich_identity_media.py clubs --api-url "$API_URL" --workers 6

echo "== Player height / preferred foot =="
python3 -u scripts/enrich_player_bio.py --api-url "$API_URL" --skip-fbref --workers 8

echo "== Full DOB (YEAR → DAY where Wikidata matches year) =="
python3 -u scripts/enrich_player_bio.py --api-url "$API_URL" --dob-only --workers 8

echo "== Player photos =="
python3 -u scripts/enrich_identity_media.py players --api-url "$API_URL" --workers 8

echo "== Done post-ingest enrich =="
PGPASSWORD="${DATABASE_PASSWORD:-kleos}" psql -h "${DATABASE_HOST:-localhost}" -U "${DATABASE_USERNAME:-kleos}" -d "${DATABASE_NAME:-kleos_transfers}" -c "
select
  (select count(*) from tournaments where deleted_at is null) as tournaments,
  (select count(*) from clubs where deleted_at is null) as clubs,
  (select count(*) from players where deleted_at is null) as players,
  (select count(crest_url) from clubs where deleted_at is null) as clubs_with_crest,
  (select count(photo_url) from players where deleted_at is null) as players_with_photo,
  (select count(height_cm) from players where deleted_at is null) as with_height,
  (select count(preferred_foot) from players where deleted_at is null) as with_foot,
  (select count(*) from players where deleted_at is null and date_of_birth_precision='DAY') as dob_day;
"
