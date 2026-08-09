#!/usr/bin/env python3
"""Backfill player_seasons.xg / xa from Understat via soccerdata.

FBref standard/shooting tables currently omit Expected columns for many seasons
(#37). Understat publishes xG/xA for the top-five leagues and is used here as a
secondary enricher — derived aggregates only; do not republish raw dumps.

Matching is best-effort on (league, season, player name). Unmatched rows are left
unchanged.

Usage:
  python3 scripts/enrich_xg_from_understat.py --seasons 2024/25 --leagues EPL
  python3 scripts/enrich_xg_from_understat.py --dry-run
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
import unicodedata
from pathlib import Path

# League maps: soccerdata Understat league ids ↔ Kleos tournament names
UNDERSTAT_LEAGUES = {
    "ENG-Premier League": "Premier League",
    "ESP-La Liga": "La Liga",
    "GER-Bundesliga": "Bundesliga",
    "ITA-Serie A": "Serie A",
    "FRA-Ligue 1": "Ligue 1",
}


def normalize_name(value: str) -> str:
    folded = unicodedata.normalize("NFKD", value)
    ascii_only = "".join(ch for ch in folded if not unicodedata.combining(ch))
    return re.sub(r"[^a-z0-9]+", "", ascii_only.lower())


def season_to_understat(label: str) -> str:
    # 2024/25 -> 2024
    return label.split("/")[0]


def connect(url: str):
    try:
        import psycopg
    except ImportError:
        try:
            import psycopg2 as psycopg  # type: ignore
        except ImportError as exc:
            raise SystemExit("Install psycopg: pip install 'psycopg[binary]'") from exc
    return psycopg.connect(url)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--database-url",
        default=os.environ.get(
            "DATABASE_URL", "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
        ),
    )
    parser.add_argument("--seasons", nargs="*", default=["2024/25", "2023/24", "2022/23"])
    parser.add_argument(
        "--leagues",
        nargs="*",
        default=list(UNDERSTAT_LEAGUES.keys()),
        help="soccerdata Understat league ids, e.g. ENG-Premier League",
    )
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--delay", type=float, default=2.0)
    args = parser.parse_args()

    try:
        import soccerdata as sd
        import pandas as pd
    except ImportError as exc:
        raise SystemExit(
            "Install ingest deps: pip install -r scripts/requirements-ingest.txt"
        ) from exc

    updated = 0
    matched = 0
    with connect(args.database_url) as conn:
        for league_key in args.leagues:
            tournament = UNDERSTAT_LEAGUES.get(league_key)
            if not tournament:
                print(f"unknown league {league_key}", file=sys.stderr)
                continue
            for season_label in args.seasons:
                year = season_to_understat(season_label)
                print(f"Fetching Understat {league_key} {year}…")
                try:
                    understat = sd.Understat(leagues=league_key, seasons=year)
                    frame = understat.read_player_season_stats()
                except Exception as exc:  # noqa: BLE001 - scrape surface is fragile
                    print(f"  skip: {exc}")
                    time.sleep(args.delay)
                    continue

                if isinstance(frame.columns, pd.MultiIndex):
                    frame.columns = [
                        "_".join(str(part) for part in col if part and str(part) != "nan").strip("_")
                        for col in frame.columns
                    ]
                frame = frame.reset_index()
                name_col = next(
                    (c for c in frame.columns if str(c).lower() in {"player", "player_name", "name"}),
                    None,
                )
                xg_col = next((c for c in frame.columns if str(c).lower() in {"xg", "x_g"}), None)
                xa_col = next(
                    (c for c in frame.columns if str(c).lower() in {"xa", "x_a", "xag"}),
                    None,
                )
                if not name_col or not xg_col:
                    print(f"  missing columns in {list(frame.columns)[:12]}")
                    continue

                lookup = {}
                for _, row in frame.iterrows():
                    key = normalize_name(str(row[name_col]))
                    lookup[key] = (
                        float(row[xg_col] or 0),
                        float(row[xa_col] or 0) if xa_col else 0.0,
                    )

                with conn.cursor() as cur:
                    cur.execute(
                        """
                        SELECT ps.id, p.full_name, ps.xg, ps.xa
                        FROM player_seasons ps
                        JOIN players p ON p.id = ps.player_id
                        JOIN seasons s ON s.id = ps.season_id
                        JOIN club_seasons cs
                          ON cs.club_id = ps.club_id
                         AND cs.season_id = ps.season_id
                         AND cs.deleted_at IS NULL
                        JOIN tournaments t ON t.id = cs.tournament_id
                        WHERE ps.deleted_at IS NULL
                          AND s.label = %s
                          AND t.name = %s
                        """,
                        (season_label, tournament),
                    )
                    targets = cur.fetchall()
                    for ps_id, full_name, old_xg, old_xa in targets:
                        stats = lookup.get(normalize_name(full_name))
                        if not stats:
                            continue
                        matched += 1
                        xg, xa = stats
                        if args.dry_run:
                            continue
                        cur.execute(
                            """
                            UPDATE player_seasons
                            SET xg = %s, xa = %s, updated_at = NOW()
                            WHERE id = %s
                            """,
                            (round(xg, 2), round(xa, 2), ps_id),
                        )
                        if float(old_xg or 0) != round(xg, 2) or float(old_xa or 0) != round(xa, 2):
                            updated += 1
                conn.commit()
                print(f"  {season_label} {tournament}: candidates={len(targets)} matched_names={matched}")
                time.sleep(args.delay)

    print(f"Done. rows_updated≈{updated} name_matches={matched} dry_run={args.dry_run}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
