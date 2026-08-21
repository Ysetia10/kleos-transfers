#!/usr/bin/env python3
"""Backfill precise pitch roles from FBref match reports.

Season-level FBref squad tables only expose GK/DF/MF/FW, which Kleos collapses to
GK/CB/CM/ST. Match summary / lineup tables use fine codes (RB, CB, CM, RW, …).

This enricher:
  1. Loads FBref player match stats (`stat_type=summary`) for selected leagues/seasons
  2. Aggregates minutes by (player, club, mapped Position)
  3. Writes the dominant precise role onto `player_seasons.primary_position`
  4. Refreshes `players.primary_position` from each player's highest-minutes season

FBref is slow on purpose (Chrome + ~10 req/min). Optimisations in this script:
  - stratified match sampling (cover every club with a small --max-matches)
  - prefer already-cached match HTML before fresh scrapes
  - batch fetch+DB apply so a long run can be interrupted safely
  - force_cache for completed seasons

Prefer `scripts/enrich_positions_from_premierleague.py` for ENG.

Usage:
  python3 scripts/enrich_positions_from_fbref_lineups.py --seasons 2024/25 --leagues ESP-La\\ Liga --max-matches 40
  python3 scripts/enrich_positions_from_fbref_lineups.py --seasons 2024/25 --max-matches 40 --sample head
"""

from __future__ import annotations

import argparse
import os
import re
import sys
import time
import unicodedata
from collections import defaultdict
from datetime import date
from pathlib import Path

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

FBREF_CACHE = Path.home() / "soccerdata" / "data" / "FBref"


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


def is_live_season_label(label: str) -> bool:
    match = re.fullmatch(r"(\d{4})/(\d{2})", label.strip())
    if not match:
        return True
    start = int(match.group(1))
    # Season S/(S+1) is live until roughly July of year S+1.
    return date.today() < date(start + 1, 7, 1)


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
    groups = [
        {"brighton", "brightonandhovealbion", "brightonhovealbion"},
        {"manchesterunited", "manchesterutd"},
        {"newcastle", "newcastleunited", "newcastleutd"},
        {"nottingham", "nottinghamforest", "nottmforest", "nottforest"},
        {"tottenham", "tottenhamhotspur"},
        {"westham", "westhamunited"},
        {"wolves", "wolverhamptonwanderers"},
        {"leicester", "leicestercity"},
        {"ipswich", "ipswichtown"},
        {"psg", "parissaintgermain", "parissg"},
        {"inter", "internazionale", "intermilan"},
        {"atleticomadrid", "atlmadrid", "atletico"},
        {"athleticclub", "athleticbilbao", "bilbao"},
        {"betis", "realbetis"},
        {"sociedad", "realsociedad"},
        {"bayernmunich", "bayern", "fcbayernmunchen", "bayernmunchen"},
        {"dortmund", "borussiadortmund"},
        {"monchengladbach", "borussiamgladbach", "gladbach"},
        {"rbleipzig", "leipzig"},
        {"koln", "fckoln", "cologne"},
        {"milan", "acmilan"},
        {"roma", "asroma"},
        {"marseille", "olympiquemarseille", "om"},
        {"lyon", "olympiquelyonnais", "ol"},
    ]
    for group in groups:
        if base in group:
            aliases |= group
    return aliases


def cached_match_ids(data_dir: Path) -> set[str]:
    if not data_dir.is_dir():
        return set()
    return {path.stem.removeprefix("match_") for path in data_dir.glob("match_*.html")}


def sample_schedule(schedule, max_matches: int, mode: str):
    """Cap matches. Prefer spreading across clubs so a small pilot still hits every side."""
    if max_matches <= 0 or len(schedule) <= max_matches:
        return schedule
    if mode == "head":
        return schedule.head(max_matches)

    work = schedule.copy()
    work["_ord"] = range(len(work))
    home_col = next((c for c in work.columns if str(c).lower() in {"home_team", "home"}), None)
    if home_col is None:
        return work.head(max_matches)

    buckets: dict[str, list] = defaultdict(list)
    for idx, row in work.iterrows():
        buckets[str(row[home_col])].append(idx)

    picked: list = []
    while len(picked) < max_matches and buckets:
        for team in list(buckets.keys()):
            if len(picked) >= max_matches:
                break
            rows = buckets[team]
            if not rows:
                buckets.pop(team, None)
                continue
            picked.append(rows.pop(0))
            if not rows:
                buckets.pop(team, None)
    return work.loc[picked].sort_values("_ord").drop(columns=["_ord"])


def apply_frame(
    conn,
    frame,
    season_label: str,
    tournament: str,
    dry_run: bool,
) -> tuple[int, int, int]:
    """Returns (precise_keys, season_updates, player_updates) for this batch."""
    import pandas as pd

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
        return 0, 0, 0

    minutes: dict[tuple[str, str], dict[str, float]] = defaultdict(lambda: defaultdict(float))
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

    primary = {key: max(by_pos.items(), key=lambda item: item[1])[0] for key, by_pos in minutes.items()}

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

        club_index: dict[str, list[tuple]] = defaultdict(list)
        for row in targets:
            for alias in club_aliases(row[3]):
                club_index[alias].append(row)

        season_updates = 0
        matched = 0
        for (player_key, team_key), pos in primary.items():
            candidates = club_index.get(team_key) or []
            if not candidates:
                for alias in club_aliases(team_key):
                    candidates = club_index.get(alias) or []
                    if candidates:
                        break
            hit = next(
                (row for row in candidates if normalize_name(row[2]) == player_key),
                None,
            )
            if hit is None:
                continue
            matched += 1
            ps_id, _player_id, _name, _club, old_pos = hit
            if old_pos == pos:
                continue
            if dry_run:
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

        player_updates = 0
        if not dry_run:
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
            player_updates = cur.rowcount

    if not dry_run:
        conn.commit()
    print(
        f"  applied role_keys={len(primary)} name_matches={matched} "
        f"season_updates≈{season_updates} player_updates≈{player_updates}"
    )
    return len(primary), season_updates, player_updates


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--database-url",
        default=os.environ.get(
            "KLEOS_DATABASE",
            os.environ.get(
                "DATABASE_URL", "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
            ),
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
    parser.add_argument(
        "--sample",
        choices=("stratified", "head"),
        default="stratified",
        help="How to pick matches when --max-matches is set (default: stratified by home club)",
    )
    parser.add_argument(
        "--batch-size",
        type=int,
        default=5,
        help="Fetch+apply this many matches at a time (safe to interrupt)",
    )
    parser.add_argument(
        "--prefer-cached",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="When capping matches, fill the quota from already-cached HTML first",
    )
    parser.add_argument("--sleep", type=float, default=1.0, help="Pause after each league-season")
    parser.add_argument(
        "--headless",
        action=argparse.BooleanOptionalAction,
        default=True,
    )
    args = parser.parse_args()

    try:
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
                force_cache = not is_live_season_label(season_label)
                print(
                    f"Fetching FBref match positions {league_key} {season_label} ({sd_season})"
                    f"{' [force_cache]' if force_cache else ''}…"
                )
                try:
                    fbref = sd.FBref(
                        leagues=league_key,
                        seasons=sd_season,
                        headless=args.headless,
                    )
                    schedule = fbref.read_schedule(force_cache=force_cache).reset_index()
                    schedule = schedule[
                        ~schedule["game_id"].isna() & ~schedule["match_report"].isnull()
                    ]
                    schedule = sample_schedule(schedule, args.max_matches, args.sample)
                    match_ids = [str(x) for x in schedule["game_id"].tolist()]

                    if args.prefer_cached and args.max_matches > 0:
                        cached = cached_match_ids(FBREF_CACHE)
                        # Prefer any cached La Liga match ids from the full schedule if present.
                        full_ids = [
                            str(x)
                            for x in fbref.read_schedule(force_cache=True)
                            .reset_index()["game_id"]
                            .dropna()
                            .astype(str)
                            .tolist()
                        ]
                        cached_in_league = [m for m in full_ids if m in cached]
                        fresh = [m for m in match_ids if m not in cached]
                        # Use cached first (instant), then stratified fresh picks.
                        ordered = cached_in_league + [m for m in fresh if m not in cached_in_league]
                        match_ids = ordered[: args.max_matches]
                        print(
                            f"  picks={len(match_ids)} "
                            f"(cached={sum(1 for m in match_ids if m in cached)}, "
                            f"fresh={sum(1 for m in match_ids if m not in cached)})"
                        )

                    if not match_ids:
                        print("  no matches with reports")
                        continue

                    batch_size = max(1, args.batch_size)
                    total_batches = (len(match_ids) + batch_size - 1) // batch_size
                    for start in range(0, len(match_ids), batch_size):
                        batch = match_ids[start : start + batch_size]
                        print(
                            f"  batch {start // batch_size + 1}/{total_batches} "
                            f"n={len(batch)} id0={batch[0]}"
                        )
                        frame = fbref.read_player_match_stats(
                            stat_type="summary",
                            match_id=batch,
                            force_cache=force_cache,
                        )
                        keys, su, pu = apply_frame(
                            conn, frame, season_label, tournament, args.dry_run
                        )
                        precise_rows += keys
                        season_updates += su
                        player_updates += pu
                except Exception as exc:  # noqa: BLE001 - scrape surface is fragile
                    print(f"  skip: {exc}")
                    time.sleep(args.sleep)
                    continue

                time.sleep(args.sleep)

    print(
        f"Done. precise_role_keys≈{precise_rows} season_updates≈{season_updates} "
        f"player_updates≈{player_updates} dry_run={args.dry_run}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
