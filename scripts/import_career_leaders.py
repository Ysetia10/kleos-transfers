#!/usr/bin/env python3
"""Import curated Wikipedia career leaders into league_career_totals.

Uses direct Postgres SQL (same DB as the backend). Matches player_id when an
exact full_name exists in players.

Usage:
  DATABASE_URL=postgresql://kleos:kleos@localhost:5432/kleos_transfers \\
    python3 scripts/import_career_leaders.py
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "research" / "career-leaders"

FILES = [
    ("PREMIER_LEAGUE", "GOALS", DATA / "premier_league_goals.csv"),
    ("PREMIER_LEAGUE", "ASSISTS", DATA / "premier_league_assists.csv"),
    ("LA_LIGA", "GOALS", DATA / "la_liga_goals.csv"),
    ("LA_LIGA", "ASSISTS", DATA / "la_liga_assists.csv"),
]


def connect(url: str):
    try:
        import psycopg
    except ImportError:
        try:
            import psycopg2 as psycopg  # type: ignore
        except ImportError as exc:
            raise SystemExit(
                "Install psycopg: pip install psycopg[binary]  (or psycopg2-binary)"
            ) from exc
    return psycopg.connect(url)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--database-url",
        default=os.environ.get(
            "DATABASE_URL", "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
        ),
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    rows: list[tuple] = []
    for league, metric, path in FILES:
        if not path.exists():
            print(f"missing {path}", file=sys.stderr)
            return 1
        with path.open(newline="", encoding="utf-8") as handle:
            for row in csv.DictReader(handle):
                rows.append(
                    (
                        str(uuid.uuid4()),
                        league,
                        metric,
                        int(row["rank"]),
                        row["player_name"].strip(),
                        int(row["total"]),
                        row["source"].strip(),
                        row.get("source_url") or None,
                        row["as_of_date"].strip(),
                    )
                )

    print(f"Prepared {len(rows)} career leader rows")
    if args.dry_run:
        return 0

    with connect(args.database_url) as conn:
        with conn.cursor() as cur:
            for league, metric, _ in FILES:
                cur.execute(
                    """
                    UPDATE league_career_totals
                    SET deleted_at = NOW(), updated_at = NOW()
                    WHERE league_code = %s AND metric = %s AND deleted_at IS NULL
                    """,
                    (league, metric),
                )
            for row in rows:
                (
                    row_id,
                    league,
                    metric,
                    rank,
                    name,
                    total,
                    source,
                    source_url,
                    as_of,
                ) = row
                cur.execute(
                    """
                    SELECT id FROM players
                    WHERE deleted_at IS NULL AND lower(full_name) = lower(%s)
                    ORDER BY created_at
                    LIMIT 1
                    """,
                    (name,),
                )
                found = cur.fetchone()
                player_id = found[0] if found else None
                cur.execute(
                    """
                    INSERT INTO league_career_totals (
                        id, league_code, metric, rank, player_name, total,
                        player_id, source, source_url, as_of_date,
                        created_at, updated_at, deleted_at
                    ) VALUES (
                        %s, %s, %s, %s, %s, %s,
                        %s, %s, %s, %s::date,
                        NOW(), NOW(), NULL
                    )
                    """,
                    (
                        row_id,
                        league,
                        metric,
                        rank,
                        name,
                        total,
                        player_id,
                        source,
                        source_url,
                        as_of,
                    ),
                )
        conn.commit()
    print("Imported career leaders.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
