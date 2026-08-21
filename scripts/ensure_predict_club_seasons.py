#!/usr/bin/env python3
"""Copy ClubSeason shells from the prior campaign into a forward predict-to season.

2026/27 has transfers but zero ClubSeason rows; compatibility / league context reads prior
ClubSeason when present. Cloning last season's Big-5 club↔tournament links keeps the
simulator usable without scraping incomplete outcomes.

Usage:
  python3 scripts/ensure_predict_club_seasons.py --dry-run
  python3 scripts/ensure_predict_club_seasons.py --from-season 2025/26 --to-season 2026/27
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request


DEFAULT_DB = "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
DEFAULT_API = "http://localhost:8080"


def psql_json(db: str, sql: str) -> list[dict]:
    out = subprocess.check_output(
        ["psql", db, "-v", "ON_ERROR_STOP=1", "-t", "-A", "-c", sql],
        text=True,
    ).strip()
    return json.loads(out or "[]")


def post_json(url: str, payload: dict) -> dict:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=120) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--db", default=os.environ.get("KLEOS_DATABASE", DEFAULT_DB))
    parser.add_argument("--api-url", default=os.environ.get("KLEOS_API_URL", DEFAULT_API))
    parser.add_argument("--from-season", default="2025/26")
    parser.add_argument("--to-season", default="2026/27")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    sql = f"""
    SELECT COALESCE(json_agg(row_to_json(q)), '[]'::json) FROM (
      SELECT
        cs.club_id::text AS "clubId",
        to_s.id::text AS "seasonId",
        cs.tournament_id::text AS "tournamentId",
        c.name AS club_name,
        t.name AS tournament_name
      FROM club_seasons cs
      JOIN clubs c ON c.id = cs.club_id AND c.deleted_at IS NULL
      JOIN seasons from_s ON from_s.id = cs.season_id AND from_s.label = '{args.from_season}'
      JOIN seasons to_s ON to_s.label = '{args.to_season}' AND to_s.deleted_at IS NULL
      JOIN tournaments t ON t.id = cs.tournament_id
      WHERE cs.deleted_at IS NULL
        AND c.country_code IN ('ENG','ESP','GER','ITA','FRA')
        AND NOT EXISTS (
          SELECT 1 FROM club_seasons existing
          WHERE existing.club_id = cs.club_id
            AND existing.season_id = to_s.id
            AND existing.deleted_at IS NULL
        )
      ORDER BY c.name
    ) q
    """
    rows = psql_json(args.db, sql)
    print(f"ClubSeason shells to create for {args.to_season}: {len(rows)} (from {args.from_season})")
    for row in rows[:5]:
        print(f"  {row['club_name']} → {row['tournament_name']}")
    if len(rows) > 5:
        print(f"  … +{len(rows) - 5} more")

    items = [
        {"clubId": r["clubId"], "seasonId": r["seasonId"], "tournamentId": r["tournamentId"]}
        for r in rows
    ]
    if args.dry_run or not items:
        return 0

    base = args.api_url.rstrip("/")
    try:
        result = post_json(f"{base}/api/v1/club-seasons/bulk", {"items": items})
    except urllib.error.HTTPError as exc:
        print(f"bulk failed: {exc.code} {exc.read().decode('utf-8', errors='replace')}", file=sys.stderr)
        return 1
    print(f"Done. created={result.get('createdCount')} skipped={result.get('skippedCount')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
