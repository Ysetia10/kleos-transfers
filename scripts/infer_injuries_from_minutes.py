#!/usr/bin/env python3
"""Infer provisional injury spells from same-club minutes collapses.

We do not yet have a licensed injury feed. Until one is wired, this script creates
MODERATE/SEVERE injury rows from unexplained YoY minutes drops at the *same* club
(not club-changers — those are usually competition, not injury).

Heuristic (top-5 leagues only):
  prior season minutes ≥ 2200
  next season minutes ≤ 45% of prior (and ≤ 1200)
  → spell ending before next season start, daysOut ≈ missing workload

Usage:
  ./scripts/infer_injuries_from_minutes.py --dry-run
  ./scripts/infer_injuries_from_minutes.py --apply
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import date, datetime, timedelta, timezone
from typing import Any


DEFAULT_DB = "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
DEFAULT_API = "http://localhost:8080"

SQL = """
WITH ranked AS (
  SELECT
    ps.player_id,
    p.full_name,
    ps.club_id,
    c.name AS club_name,
    c.country_code,
    s.label AS season_label,
    s.start_date,
    s.end_date,
    ps.minutes_played,
    LAG(ps.minutes_played) OVER (
      PARTITION BY ps.player_id, ps.club_id
      ORDER BY s.start_date
    ) AS prior_minutes,
    LAG(s.label) OVER (
      PARTITION BY ps.player_id, ps.club_id
      ORDER BY s.start_date
    ) AS prior_label,
    LAG(s.end_date) OVER (
      PARTITION BY ps.player_id, ps.club_id
      ORDER BY s.start_date
    ) AS prior_end
  FROM player_seasons ps
  JOIN players p ON p.id = ps.player_id AND p.deleted_at IS NULL
  JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
  JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
  WHERE c.country_code IN ('ENG', 'ESP', 'GER', 'ITA', 'FRA')
    AND ps.deleted_at IS NULL
    -- GK minute collapses are usually shirt loss, not injury
    AND ps.primary_position <> 'GK'
)
SELECT
  player_id::text,
  full_name,
  club_name,
  country_code,
  prior_label,
  season_label,
  prior_minutes,
  minutes_played AS next_minutes,
  start_date::text AS low_season_start,
  end_date::text AS low_season_end
FROM ranked
WHERE prior_minutes IS NOT NULL
  AND prior_minutes >= 2200
  AND minutes_played <= 1200
  AND minutes_played::float / prior_minutes <= 0.45
  -- consecutive seasons only (skip multi-year gaps / loans away)
  AND prior_end IS NOT NULL
  AND (start_date - prior_end) <= 40
  -- exclude mid-season departures (minutes collapse is a transfer, not injury)
  AND NOT EXISTS (
    SELECT 1
    FROM transfers t
    WHERE t.player_id = ranked.player_id
      AND t.from_club_id = ranked.club_id
      AND t.deleted_at IS NULL
      AND t.transfer_date >= ranked.start_date
      AND t.transfer_date <= ranked.end_date
  )
  -- exclude same-season minutes at another club (loan/sale mid-season)
  AND NOT EXISTS (
    SELECT 1
    FROM player_seasons ps2
    JOIN seasons s2 ON s2.id = ps2.season_id AND s2.deleted_at IS NULL
    WHERE ps2.player_id = ranked.player_id
      AND ps2.club_id <> ranked.club_id
      AND s2.label = ranked.season_label
      AND ps2.minutes_played >= 900
      AND ps2.deleted_at IS NULL
  )
ORDER BY prior_minutes - minutes_played DESC
"""


def connect(url: str):
    try:
        import psycopg

        return psycopg.connect(url)
    except ImportError:
        try:
            import psycopg2 as psycopg  # type: ignore

            return psycopg.connect(url)
        except ImportError as exc:
            raise SystemExit("Install psycopg: pip install 'psycopg[binary]'") from exc


def severity_for_days(days: int) -> str:
    if days >= 90:
        return "SEVERE"
    if days >= 21:
        return "MODERATE"
    return "MINOR"


def build_payload(row: dict[str, Any]) -> dict[str, Any]:
    """Date the spell *inside* the low-minutes season so as-of season-start backtests do not leak.

    Predicting the low season itself will not see this injury (starts after asOf).
    Predicting later seasons will see it in the 12-month lookback — correct.
    """
    prior = int(row["prior_minutes"])
    nxt = int(row["next_minutes"])
    missing = max(0, prior - nxt)
    days = int(min(180, max(21, missing / 90.0 * 7)))
    low_start = date.fromisoformat(str(row["low_season_start"])[:10])
    low_end = date.fromisoformat(str(row["low_season_end"])[:10])
    # Start ~2 months into the low season (after asOf = July 1).
    start = low_start + timedelta(days=60)
    end = min(start + timedelta(days=days - 1), low_end - timedelta(days=1))
    if end <= start:
        end = start + timedelta(days=max(13, days // 2))
    return {
        "playerId": row["player_id"],
        "injuryType": "Inferred availability gap",
        "severity": severity_for_days((end - start).days + 1),
        "startDate": start.isoformat(),
        "endDate": end.isoformat(),
    }


def post_bulk(api_url: str, items: list[dict[str, Any]]) -> dict[str, Any]:
    body = json.dumps({"items": items}).encode("utf-8")
    req = urllib.request.Request(
        f"{api_url.rstrip('/')}/api/v1/injuries/bulk",
        data=body,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read())
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"bulk import failed HTTP {error.code}: {detail}") from error


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--database", default=os.environ.get("KLEOS_DATABASE", DEFAULT_DB))
    parser.add_argument("--api-url", default=os.environ.get("KLEOS_API_URL", DEFAULT_API))
    parser.add_argument("--limit", type=int, default=0, help="Cap inferred rows (0 = all)")
    parser.add_argument("--dry-run", action="store_true", help="Print only; do not POST")
    parser.add_argument("--apply", action="store_true", help="POST inferred injuries to the API")
    parser.add_argument("--batch-size", type=int, default=200)
    args = parser.parse_args()
    if not args.dry_run and not args.apply:
        raise SystemExit("Pass --dry-run or --apply")

    with connect(args.database) as conn:
        with conn.cursor() as cur:
            cur.execute(SQL)
            cols = [d[0] for d in cur.description]
            rows = [dict(zip(cols, r)) for r in cur.fetchall()]

    if args.limit > 0:
        rows = rows[: args.limit]

    payloads = [build_payload(r) for r in rows]
    print(f"Inferred {len(payloads)} injury spells from same-club minutes collapses")
    for row, payload in list(zip(rows, payloads))[:15]:
        print(
            f"  {row['full_name']:28} {row['club_name']:22} "
            f"{row['prior_label']}→{row['season_label']} "
            f"{row['prior_minutes']}→{row['next_minutes']} "
            f"{payload['severity']} {payload['startDate']}…{payload['endDate']}"
        )
    if len(payloads) > 15:
        print(f"  … and {len(payloads) - 15} more")

    if args.dry_run:
        return

    created = 0
    skipped = 0
    failed = 0
    for i in range(0, len(payloads), args.batch_size):
        chunk = payloads[i : i + args.batch_size]
        result = post_bulk(args.api_url, chunk)
        if isinstance(result, dict):
            created += int(result.get("createdCount", 0))
            skipped += int(result.get("skippedCount", 0))
            failed += int(result.get("failedCount", 0))
            print(
                f"  batch {i // args.batch_size + 1}: "
                f"created={result.get('createdCount', 0)} "
                f"skipped={result.get('skippedCount', 0)} "
                f"failed={result.get('failedCount', 0)}"
            )
            for issue in (result.get("failed") or [])[:5]:
                print(f"    fail: {issue}")
        else:
            created += len(chunk)
            print(f"  posted batch {i // args.batch_size + 1}: {len(chunk)} items")
    print(
        f"Done. created={created} skipped={skipped} failed={failed} "
        f"at {datetime.now(timezone.utc).isoformat()}"
    )


if __name__ == "__main__":
    main()
