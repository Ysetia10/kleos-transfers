#!/usr/bin/env python3
"""Backfill precise pitch roles from FBref match reports.

Season-level FBref squad tables only expose GK/DF/MF/FW, which Kleos collapses to
GK/CB/CM/ST. Match summary / lineup tables use fine codes (RB, CB, CM, RW, …).

This enricher:
  1. Loads FBref player match stats (`stat_type=summary`) for selected leagues/seasons
  2. Aggregates minutes by (player, club, mapped Position)
  3. Writes the dominant precise role onto `player_seasons.primary_position`
  4. Refreshes `players.primary_position` from each player's highest-minutes season

Coarse roles are left unchanged when no precise match minutes are found.

Usage:
  python3 scripts/enrich_positions_from_fbref_lineups.py --seasons 2024/25 --leagues ENG-Premier League --dry-run
  python3 scripts/enrich_positions_from_fbref_lineups.py --seasons 2024/25 --max-matches 40
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
import unicodedata
from collections import defaultdict

# Keep in sync with scripts/ingest_fbref_pl_laliga.py POSITION_MAP.
POSITION_MAP = {
    "GK": "GK",
    "DF": "CB",
    "MF": "CM",
    "FW": "ST",
    "CB": "CB",
    "RB": "RB",
    "LB": "LB",
    "RWB": "RWB",
    "LWB": "LWB",
    "DM": "CDM",
    "CDM": "CDM",
    "CM": "CM",
    "AM": "CAM",
    "CAM": "CAM",
    "RM": "RM",
    "LM": "LM",
    "RW": "RW",
    "LW": "LW",
    "CF": "CF",
    "ST": "ST",
    "SS": "CF",
    "WB": "RWB",
}

COARSE = {"GK", "CB", "CM", "ST"}

LEAGUES = {
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


def map_position(raw: object) -> str | None:
    if raw is None:
        return None
    text = str(raw).split(",")[0].strip().upper()
    if not text or text == "NAN":
        return None
    mapped = POSITION_MAP.get(text)
    if mapped is None:
        return None
    # Only accept precise codes from match reports; ignore coarse DF/MF/FW leftovers.
    if mapped in COARSE and text in {"DF", "MF", "FW"}:
        return None
    return mapped


def label_to_soccerdata_season(label: str) -> str:
    match = re.fullmatch(r"(\d{4})/(\d{2})", label.strip())
    if not match:
        raise SystemExit(f"Invalid season label {label!r}; expected YYYY/YY")
    start = int(match.group(1))
    return f"{str(start)[2:]}{str(start + 1)[2:]}"


def connect(url: str):
    try:
        import psycopg
    except ImportError:
        try:
            import psycopg2 as psycopg  # type: ignore
        except ImportError as exc:
            raise SystemExit("Install psycopg: pip install 'psycopg[binary]'") from exc
    return psycopg.connect(url)


def club_aliases(name: str) -> set[str]:
    base = normalize_name(name)
    aliases = {base}
    # FBref short forms commonly used in match tables.
    replacements = {
        "manchesterunited": "manchesterutd",
        "manchesterutd": "manchesterunited",
        "tottenhamhotspur": "tottenham",
        "wolverhamptonwanderers": "wolves",
        "brightonandhovealbion": "brighton",
        "nottinghamforest": "nottforest",
        "newcastleunited": "newcastleutd",
        "westbromwichalbion": "westbrom",
        "paris saintgermain": "psg",
        "parissaintgermain": "psg",
        "atleticomadrid": "atleticomadrid",
        "intermilan": "internazionale",
        "internazionale": "inter",
    }
    if base in replacements:
        aliases.add(replacements[base])
    for src, dst in replacements.items():
        if base == dst:
            aliases.add(src)
    return aliases


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--database-url",
        default=os.environ.get(
            "DATABASE_URL", "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
        ),
    )
    parser.add_argument("--seasons", nargs="*", default=["2024/25"])
    parser.add_argument(
        "--leagues",
        nargs="*",
        default=list(LEAGUES.keys()),
        help="soccerdata FBref league ids",
    )
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument(
        "--max-matches",
        type=int,
        default=0,
        help="Optional cap on matches per league-season (0 = all)",
    )
    parser.add_argument("--sleep", type=float, default=1.0, help="Pause after each league-season")
    parser.add_argument(
        "--headless",
        action=argparse.BooleanOptionalAction,
        default=True,
    )
    args = parser.parse_args()

    try:
        import pandas as pd
        import soccerdata as sd
    except ImportError as exc:
        raise SystemExit(
            "Install ingest deps: pip install -r scripts/requirements-ingest.txt"
        ) from exc

    season_updates = 0
    player_updates = 0
    precise_rows = 0

    with connect(args.database_url) as conn:
        for league_key in args.leagues:
            tournament = LEAGUES.get(league_key)
            if not tournament:
                print(f"unknown league {league_key}", file=sys.stderr)
                continue
            for season_label in args.seasons:
                sd_season = label_to_soccerdata_season(season_label)
                print(f"Fetching FBref match positions {league_key} {season_label} ({sd_season})…")
                try:
                    fbref = sd.FBref(
                        leagues=league_key,
                        seasons=sd_season,
                        headless=args.headless,
                    )
                    schedule = fbref.read_schedule().reset_index()
                    schedule = schedule[
                        ~schedule["game_id"].isna() & ~schedule["match_report"].isnull()
                    ]
                    if args.max_matches > 0:
                        schedule = schedule.head(args.max_matches)
                    match_ids = schedule["game_id"].astype(str).tolist()
                    if not match_ids:
                        print("  no matches with reports")
                        continue
                    frame = fbref.read_player_match_stats(
                        stat_type="summary",
                        match_id=match_ids,
                    )
                except Exception as exc:  # noqa: BLE001 - scrape surface is fragile
                    print(f"  skip: {exc}")
                    time.sleep(args.sleep)
                    continue

                frame = frame.reset_index()
                if isinstance(frame.columns, pd.MultiIndex):
                    frame.columns = [
                        "_".join(str(part) for part in col if part and str(part) != "nan").strip("_")
                        for col in frame.columns
                    ]

                player_col = next(
                    (c for c in frame.columns if str(c).lower() in {"player", "player_name"}),
                    None,
                )
                team_col = next((c for c in frame.columns if str(c).lower() == "team"), None)
                pos_col = next((c for c in frame.columns if str(c).lower() in {"pos", "position"}), None)
                min_col = next((c for c in frame.columns if str(c).lower() in {"min", "minutes"}), None)
                if not player_col or not team_col or not pos_col or not min_col:
                    print(f"  missing columns in {list(frame.columns)[:16]}")
                    continue

                # minutes by (player, team) -> position
                minutes: dict[tuple[str, str], dict[str, float]] = defaultdict(
                    lambda: defaultdict(float)
                )
                for _, row in frame.iterrows():
                    mapped = map_position(row[pos_col])
                    if mapped is None:
                        continue
                    try:
                        mins = float(row[min_col] or 0)
                    except (TypeError, ValueError):
                        mins = 0.0
                    if mins <= 0:
                        continue
                    key = (normalize_name(str(row[player_col])), normalize_name(str(row[team_col])))
                    minutes[key][mapped] += mins

                primary: dict[tuple[str, str], str] = {}
                for key, by_pos in minutes.items():
                    best = max(by_pos.items(), key=lambda item: item[1])
                    if best[0] not in COARSE or best[0] == "GK":
                        primary[key] = best[0]
                        precise_rows += 1
                    elif best[0] in {"CB", "CM", "ST"} and len(by_pos) == 1:
                        # Keep precise GK; for single-bucket CB/CM/ST from codes like CB/CM/ST keep them.
                        primary[key] = best[0]
                        precise_rows += 1
                    else:
                        primary[key] = best[0]
                        precise_rows += 1

                with conn.cursor() as cur:
                    cur.execute(
                        """
                        SELECT ps.id, p.id, p.full_name, c.name, ps.primary_position
                        FROM player_seasons ps
                        JOIN players p ON p.id = ps.player_id
                        JOIN clubs c ON c.id = ps.club_id
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

                    # Index club aliases once.
                    club_index: dict[str, list[tuple]] = defaultdict(list)
                    for row in targets:
                        ps_id, player_id, full_name, club_name, old_pos = row
                        for alias in club_aliases(club_name):
                            club_index[alias].append(row)

                    matched = 0
                    for (player_key, team_key), pos in primary.items():
                        candidates = club_index.get(team_key) or []
                        hit = next(
                            (
                                row
                                for row in candidates
                                if normalize_name(row[2]) == player_key
                            ),
                            None,
                        )
                        if hit is None:
                            continue
                        matched += 1
                        ps_id, player_id, _name, _club, old_pos = hit
                        if old_pos == pos:
                            continue
                        if args.dry_run:
                            season_updates += 1
                            continue
                        cur.execute(
                            """
                            UPDATE player_seasons
                            SET primary_position = %s, updated_at = NOW()
                            WHERE id = %s
                            """,
                            (pos, ps_id),
                        )
                        season_updates += 1

                    # Refresh player identity position from highest-minutes season that is precise.
                    if not args.dry_run:
                        cur.execute(
                            """
                            UPDATE players p
                            SET primary_position = src.primary_position,
                                updated_at = NOW()
                            FROM (
                                SELECT DISTINCT ON (ps.player_id)
                                       ps.player_id,
                                       ps.primary_position
                                FROM player_seasons ps
                                JOIN seasons s ON s.id = ps.season_id
                                WHERE ps.deleted_at IS NULL
                                  AND ps.primary_position IS NOT NULL
                                ORDER BY ps.player_id, ps.minutes_played DESC NULLS LAST, s.start_date DESC
                            ) src
                            WHERE p.id = src.player_id
                              AND p.deleted_at IS NULL
                              AND p.primary_position IS DISTINCT FROM src.primary_position
                              AND p.id IN (
                                  SELECT DISTINCT ps.player_id
                                  FROM player_seasons ps
                                  JOIN seasons s ON s.id = ps.season_id
                                  JOIN club_seasons cs
                                    ON cs.club_id = ps.club_id
                                   AND cs.season_id = ps.season_id
                                   AND cs.deleted_at IS NULL
                                  JOIN tournaments t ON t.id = cs.tournament_id
                                  WHERE s.label = %s AND t.name = %s
                              )
                            """,
                            (season_label, tournament),
                        )
                        player_updates += cur.rowcount

                conn.commit()
                print(
                    f"  {season_label} {tournament}: matches={len(match_ids)} "
                    f"role_keys={len(primary)} season_rows={len(targets)} name_matches={matched}"
                )
                time.sleep(args.sleep)

    print(
        f"Done. precise_role_keys≈{precise_rows} season_updates≈{season_updates} "
        f"player_updates≈{player_updates} dry_run={args.dry_run}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
