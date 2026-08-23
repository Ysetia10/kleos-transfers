#!/usr/bin/env python3
"""Export high-minute players with inferred injury spells for manual curation.

Targets: players with an active inferred availability gap and prior-season ≥2200 min
at the same club (same cohort as infer_injuries_from_minutes.py).

Usage:
  python3 scripts/export_injury_targets.py --output research/injuries/targets.csv
"""

from __future__ import annotations

import argparse
import csv
import os
import sys

DEFAULT_DB = "postgresql://kleos:kleos@localhost:5432/kleos_transfers"

SQL = """
SELECT
  p.id::text AS player_id,
  p.full_name,
  c.country_code,
  c.name AS club_name,
  i.start_date::text,
  i.end_date::text,
  i.severity::text,
  i.id::text AS inferred_injury_id
FROM injuries i
JOIN players p ON p.id = i.player_id AND p.deleted_at IS NULL
JOIN player_seasons ps ON ps.player_id = p.id AND ps.deleted_at IS NULL
JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
WHERE i.deleted_at IS NULL
  AND i.injury_type = 'Inferred availability gap'
  AND c.country_code IN ('ENG', 'ESP', 'GER', 'ITA', 'FRA')
  AND ps.minutes_played >= 2200
GROUP BY p.id, p.full_name, c.country_code, c.name, i.start_date, i.end_date, i.severity, i.id
ORDER BY p.full_name, i.start_date
"""


def connect(url: str):
    try:
        import psycopg

        return psycopg.connect(url)
    except ImportError:
        import psycopg2 as psycopg  # type: ignore

        return psycopg.connect(url)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--output", default="research/injuries/targets.csv")
    parser.add_argument("--database-url", default=os.environ.get("KLEOS_DATABASE", DEFAULT_DB))
    args = parser.parse_args()

    with connect(args.database_url) as conn:
        with conn.cursor() as cur:
            cur.execute(SQL)
            rows = cur.fetchall()

    fieldnames = [
        "playerId",
        "playerFullName",
        "countryCode",
        "clubName",
        "inferredStartDate",
        "inferredEndDate",
        "inferredSeverity",
        "inferredInjuryId",
    ]
    with open(args.output, "w", newline="", encoding="utf-8") as handle:
        writer = csv.writer(handle)
        writer.writerow(fieldnames)
        writer.writerows(rows)

    print(f"Wrote {len(rows)} rows to {args.output}")


if __name__ == "__main__":
    main()
