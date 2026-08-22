#!/usr/bin/env python3
"""Infer provisional player contracts from continuous PlayerSeason tenures.

We have no licensed contract feed. Until one exists, reconstruct club tenures:

- Start = first consecutive season start at a club (Big-5 only)
- End = last consecutive season end, unless a FREE exit / outbound transfer
  ends the spell earlier (dated at that move)

These rows power CompatibilityScorer deal-friction and ConfidenceScorer.
They are provisional (no release clause).

Usage:
  python3 scripts/infer_contracts_from_seasons.py --dry-run
  python3 scripts/infer_contracts_from_seasons.py --apply --min-minutes 450
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
DEFAULT_API = os.environ.get("API_URL", "http://localhost:8080")

SQL = """
WITH ordered AS (
  SELECT
    ps.player_id,
    ps.club_id,
    ps.minutes_played,
    s.start_date,
    s.end_date,
    LAG(ps.club_id) OVER (PARTITION BY ps.player_id ORDER BY s.start_date) AS prev_club,
    LAG(s.end_date) OVER (PARTITION BY ps.player_id ORDER BY s.start_date) AS prev_end
  FROM player_seasons ps
  JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
  JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
  WHERE ps.deleted_at IS NULL
    AND c.country_code IN ('ENG','ESP','GER','ITA','FRA')
    AND ps.minutes_played >= %(min_minutes)s
),
streaked AS (
  SELECT *,
    SUM(CASE
      WHEN prev_club IS DISTINCT FROM club_id THEN 1
      WHEN prev_end IS NOT NULL AND (start_date - prev_end) > 40 THEN 1
      ELSE 0
    END) OVER (
      PARTITION BY player_id ORDER BY start_date
      ROWS UNBOUNDED PRECEDING
    ) AS streak
  FROM ordered
),
tenures AS (
  SELECT
    player_id::text,
    club_id::text,
    MIN(start_date)::text AS start_date,
    MAX(end_date)::text AS end_date,
    SUM(minutes_played)::int AS minutes,
    COUNT(*)::int AS seasons
  FROM streaked
  GROUP BY player_id, club_id, streak
),
exits AS (
  SELECT
    t.player_id::text,
    t.from_club_id::text AS club_id,
    MIN(t.transfer_date)::text AS exit_date
  FROM transfers t
  WHERE t.deleted_at IS NULL
    AND t.status = 'COMPLETED'
    AND t.from_club_id IS NOT NULL
    AND (t.to_club_id IS NULL OR t.to_club_id IS DISTINCT FROM t.from_club_id)
  GROUP BY 1, 2
)
SELECT
  tenures.player_id,
  tenures.club_id,
  tenures.start_date,
  CASE
    WHEN exits.exit_date IS NOT NULL
         AND exits.exit_date::date > tenures.start_date::date
         AND exits.exit_date::date < tenures.end_date::date
      THEN exits.exit_date
    ELSE tenures.end_date
  END AS end_date,
  tenures.minutes,
  tenures.seasons
FROM tenures
LEFT JOIN exits
  ON exits.player_id = tenures.player_id AND exits.club_id = tenures.club_id
WHERE (
  CASE
    WHEN exits.exit_date IS NOT NULL
         AND exits.exit_date::date > tenures.start_date::date
         AND exits.exit_date::date < tenures.end_date::date
      THEN exits.exit_date::date
    ELSE tenures.end_date::date
  END
) > tenures.start_date::date
ORDER BY tenures.minutes DESC
"""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--db", default=os.environ.get("KLEOS_DATABASE", DEFAULT_DB))
    parser.add_argument("--api-url", default=DEFAULT_API)
    parser.add_argument("--min-minutes", type=int, default=450)
    parser.add_argument("--limit", type=int, default=0)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--batch-size", type=int, default=200)
    return parser.parse_args()


def fetch_rows(db: str, min_minutes: int) -> list[dict]:
    sql = SQL.replace("%(min_minutes)s", str(int(min_minutes)))
    wrapped = (
        "SELECT COALESCE(json_agg(row_to_json(q)), '[]'::json) FROM ("
        + sql
        + ") q"
    )
    out = subprocess.check_output(
        ["psql", db, "-v", "ON_ERROR_STOP=1", "-t", "-A", "-c", wrapped],
        text=True,
    ).strip()
    return json.loads(out or "[]")


def http_json(method: str, url: str, body: dict | None = None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else None


def main() -> int:
    args = parse_args()
    if not args.dry_run and not args.apply:
        print("Pass --dry-run or --apply", file=sys.stderr)
        return 2

    rows = fetch_rows(args.db, args.min_minutes)
    if args.limit > 0:
        rows = rows[: args.limit]

    items = [
        {
            "playerId": row["player_id"],
            "clubId": row["club_id"],
            "startDate": row["start_date"],
            "endDate": row["end_date"],
            "releaseClauseEur": None,
        }
        for row in rows
    ]
    print(f"Inferred contracts: {len(items)} (min_minutes={args.min_minutes})")
    for row in rows[:12]:
        print(
            f"  {row['player_id'][:8]}… club={row['club_id'][:8]}… "
            f"{row['start_date']}→{row['end_date']} seasons={row['seasons']} min={row['minutes']}"
        )
    if len(rows) > 12:
        print(f"  … +{len(rows) - 12} more")

    if args.dry_run or not items:
        return 0

    created = skipped = failed = 0
    for i in range(0, len(items), args.batch_size):
        chunk = items[i : i + args.batch_size]
        try:
            resp = http_json(
                "POST",
                f"{args.api_url.rstrip('/')}/api/v1/contracts/bulk",
                {"items": chunk},
            )
        except urllib.error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            print(f"  batch {i // args.batch_size + 1} HTTP {exc.code}: {detail[:300]}", file=sys.stderr)
            failed += len(chunk)
            continue
        created += int(resp.get("createdCount") or 0)
        skipped += int(resp.get("skippedCount") or 0)
        print(
            f"  batch {i // args.batch_size + 1}: "
            f"created={resp.get('createdCount')} skipped={resp.get('skippedCount')}"
        )

    print(f"Done. created={created} skipped={skipped} failed={failed}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
