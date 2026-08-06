#!/usr/bin/env python3
"""Ingest Premier League + La Liga history (2016/17–2025/26) from FBref into Kleos.

This is the real data path (not a demo seed). It:
  1. Ensures Tournament + Season identity rows
  2. Fetches player-season stats via soccerdata's FBref connector
  3. Upserts Clubs and Players with stable fbrefId keys (idempotent bulk)
  4. Upserts ClubSeason + PlayerSeason rows

Prerequisites:
  - Backend running with migrations applied
  - pip install -r scripts/requirements-ingest.txt
  - Read docs/data-sourcing.md before a full run

Examples:
  # Parse + print counts only
  ./scripts/ingest_fbref_pl_laliga.py --dry-run --seasons 2024/25

  # One season each league
  ./scripts/ingest_fbref_pl_laliga.py --seasons 2024/25 --api-url http://localhost:8080

  # Full window 2016/17 … 2025/26
  ./scripts/ingest_fbref_pl_laliga.py
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]

LEAGUES = {
    "ENG-Premier League": {
        "tournament_name": "Premier League",
        "tournament_short": "EPL",
        "country_code": "ENG",
        "club_country": "ENG",
    },
    "ESP-La Liga": {
        "tournament_name": "La Liga",
        "tournament_short": "LAL",
        "country_code": "ESP",
        "club_country": "ESP",
    },
}

# Inclusive European labels. soccerdata uses the starting calendar year ("2016" → 2016/17).
DEFAULT_SEASON_LABELS = [f"{year}/{str(year + 1)[2:]}" for year in range(2016, 2026)]

POSITION_MAP = {
    "GK": "GK",
    "DF": "CB",
    "MF": "CM",
    "FW": "ST",
}


@dataclass
class Counters:
    clubs: int = 0
    players: int = 0
    club_seasons: int = 0
    player_seasons: int = 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-url", default="http://localhost:8080", help="Kleos API base URL")
    parser.add_argument(
        "--seasons",
        default=",".join(DEFAULT_SEASON_LABELS),
        help="Comma-separated European labels, e.g. 2016/17,2024/25",
    )
    parser.add_argument(
        "--leagues",
        default=",".join(LEAGUES),
        help="Comma-separated soccerdata league ids",
    )
    parser.add_argument("--dry-run", action="store_true", help="Fetch/transform only; do not call the API")
    parser.add_argument("--sleep", type=float, default=1.0, help="Pause between FBref season fetches")
    parser.add_argument("--batch-size", type=int, default=200, help="Bulk API batch size (max 500)")
    return parser.parse_args()


def label_to_start_year(label: str) -> int:
    match = re.fullmatch(r"(\d{4})/(\d{2})", label.strip())
    if not match:
        raise SystemExit(f"Invalid season label {label!r}; expected YYYY/YY")
    return int(match.group(1))


def season_dates(label: str) -> tuple[str, str]:
    start_year = label_to_start_year(label)
    return f"{start_year}-07-01", f"{start_year + 1}-06-30"


def slug(value: str) -> str:
    cleaned = re.sub(r"[^a-z0-9]+", "-", value.strip().lower())
    return cleaned.strip("-") or "unknown"


def map_position(raw: Any) -> str:
    if raw is None or (isinstance(raw, float) and str(raw) == "nan"):
        return "CM"
    text = str(raw).split(",")[0].strip().upper()
    return POSITION_MAP.get(text, "CM")


def map_nation(raw: Any) -> str | None:
    if raw is None:
        return None
    text = str(raw).strip().upper()
    if not text or text == "NAN":
        return None
    # FBref sometimes returns "eng ENG" / "es ESP"
    parts = text.replace(",", " ").split()
    for part in reversed(parts):
        if re.fullmatch(r"[A-Z]{3}", part):
            return part
    return None


def safe_int(value: Any, default: int = 0) -> int:
    try:
        if value is None or (isinstance(value, float) and str(value) == "nan"):
            return default
        return int(float(value))
    except (TypeError, ValueError):
        return default


def safe_float(value: Any, default: float = 0.0) -> float:
    try:
        if value is None or (isinstance(value, float) and str(value) == "nan"):
            return default
        return float(value)
    except (TypeError, ValueError):
        return default


def flatten_columns(df):
    """Flatten MultiIndex / tuple columns from soccerdata into unique simple names."""
    out = df.copy()
    flat: list[str] = []
    for column in out.columns:
        if isinstance(column, tuple):
            parts = [
                str(part).strip()
                for part in column
                if part is not None
                and str(part).strip()
                and str(part) != "nan"
                and not str(part).startswith("Unnamed")
            ]
            flat.append(parts[-1] if parts else "value")
        else:
            flat.append(str(column))

    # Deduplicate names such as Performance Gls vs Per 90 Minutes Gls.
    seen: dict[str, int] = {}
    unique: list[str] = []
    for name in flat:
        count = seen.get(name, 0)
        seen[name] = count + 1
        unique.append(name if count == 0 else f"{name}_{count}")
    out.columns = unique
    return out


def col(df, *candidates: str):
    lower = {str(c).lower(): c for c in df.columns}
    for name in candidates:
        if name.lower() in lower:
            return lower[name.lower()]
    return None


def http_json(method: str, url: str, body: dict | None = None) -> Any:
    data = None if body is None else json.dumps(body).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Accept": "application/json", "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} -> HTTP {error.code}: {detail}") from error


def bulk_post(api_url: str, path: str, items: list[dict], batch_size: int, dry_run: bool) -> None:
    if not items:
        return
    if dry_run:
        print(f"  dry-run: would POST {len(items)} item(s) to {path}")
        return
    for start in range(0, len(items), batch_size):
        batch = items[start : start + batch_size]
        result = http_json("POST", f"{api_url}{path}/bulk", {"items": batch})
        print(
            f"  {path}: requested={result.get('requested')} "
            f"created={result.get('createdCount')} "
            f"skipped={result.get('skippedCount')} "
            f"failed={result.get('failedCount')}"
        )
        for issue in result.get("failed", [])[:10]:
            print(
                f"    fail[{issue.get('index')}] {issue.get('reference')}: "
                f"{issue.get('reason')}"
            )


def ensure_identity_page(api_url: str, path: str, size: int = 200) -> list[dict]:
    page = 0
    rows: list[dict] = []
    while True:
        payload = http_json("GET", f"{api_url}{path}?page={page}&size={size}")
        content = payload.get("content", [])
        rows.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
    return rows


def ensure_tournaments(api_url: str, dry_run: bool, batch_size: int) -> dict[str, str]:
    items = [
        {
            "name": meta["tournament_name"],
            "shortName": meta["tournament_short"],
            "confederation": "UEFA",
            "type": "LEAGUE",
            "countryCode": meta["country_code"],
        }
        for meta in LEAGUES.values()
    ]
    bulk_post(api_url, "/api/v1/tournaments", items, batch_size, dry_run)
    if dry_run:
        return {meta["tournament_name"]: f"dry-{meta['tournament_short']}" for meta in LEAGUES.values()}
    rows = ensure_identity_page(api_url, "/api/v1/tournaments")
    return {row["name"]: row["id"] for row in rows}


def ensure_seasons(api_url: str, labels: list[str], dry_run: bool, batch_size: int) -> dict[str, str]:
    items = []
    for label in labels:
        start, end = season_dates(label)
        items.append({"label": label, "startDate": start, "endDate": end})
    bulk_post(api_url, "/api/v1/seasons", items, batch_size, dry_run)
    if dry_run:
        return {label: f"dry-{label}" for label in labels}
    rows = ensure_identity_page(api_url, "/api/v1/seasons")
    return {row["label"]: row["id"] for row in rows}


def fetch_league_season(league_id: str, start_year: int):
    try:
        import soccerdata as sd
    except ImportError as error:
        raise SystemExit(
            "soccerdata is required. Install with:\n  pip install -r scripts/requirements-ingest.txt"
        ) from error

    fbref = sd.FBref(leagues=league_id, seasons=str(start_year))
    standard = flatten_columns(fbref.read_player_season_stats(stat_type="standard").reset_index())
    try:
        shooting = flatten_columns(fbref.read_player_season_stats(stat_type="shooting").reset_index())
    except Exception:
        shooting = None
    return standard, shooting


def merge_xg(standard, shooting):
    frame = standard.copy()
    xg_col = col(frame, "xG", "npxG")
    xa_col = col(frame, "xAG", "xA")
    if xg_col and xa_col:
        return frame

    keys = [name for name in ("league", "season", "team", "player") if name in frame.columns]
    if shooting is not None and keys and all(name in shooting.columns for name in keys):
        xg_s = col(shooting, "xG", "npxG")
        xa_s = col(shooting, "xAG", "xA")
        keep = keys + [name for name in (xg_s, xa_s) if name]
        if len(keep) > len(keys):
            frame = frame.merge(
                shooting[keep].drop_duplicates(keys),
                on=keys,
                how="left",
                suffixes=("", "_shot"),
            )

    if col(frame, "xG", "npxG") is None:
        frame["xG"] = 0.0
    if col(frame, "xAG", "xA") is None:
        frame["xAG"] = 0.0
    return frame


def build_rows_for_frame(df, league_id: str, label: str, meta: dict) -> tuple[list[dict], list[dict], list[dict]]:
    player_col = col(df, "player")
    team_col = col(df, "team")
    if not player_col or not team_col:
        raise RuntimeError(f"Unexpected FBref columns for {league_id} {label}: {list(df.columns)}")

    nation_col = col(df, "nation")
    pos_col = col(df, "pos")
    born_col = col(df, "born")
    age_col = col(df, "age")
    mp_col = col(df, "MP", "Apps")
    min_col = col(df, "Min", "Minutes")
    gls_col = col(df, "Gls", "Goals")
    ast_col = col(df, "Ast", "Assists")
    xg_col = col(df, "xG", "npxG")
    xa_col = col(df, "xAG", "xA")

    clubs: dict[str, dict] = {}
    players: dict[str, dict] = {}
    seasons_rows: list[dict] = []

    start_year = label_to_start_year(label)

    for _, row in df.iterrows():
        player_name = str(row[player_col]).strip()
        team_name = str(row[team_col]).strip()
        if not player_name or player_name.lower() == "nan" or not team_name or team_name.lower() == "nan":
            continue

        nation = map_nation(row[nation_col]) if nation_col else meta["club_country"]
        if nation is None:
            nation = meta["club_country"]

        born = safe_int(row[born_col], 0) if born_col else 0
        if born < 1960:
            age = safe_int(row[age_col], 0) if age_col else 0
            born = start_year - age if age > 0 else start_year - 22
        dob = f"{born}-07-01"

        club_fbref = f"fbref-club:{slug(team_name)}:{meta['club_country']}"
        player_fbref = f"fbref-player:{slug(player_name)}:{born}:{nation}"

        clubs[club_fbref] = {
            "name": team_name[:120],
            "shortName": re.sub(r"[^A-Za-z0-9]", "", team_name)[:3].upper() or "CLB",
            "countryCode": meta["club_country"],
            "foundedYear": None,
            "fbrefId": club_fbref,
        }
        players[player_fbref] = {
            "fullName": player_name[:100],
            "dateOfBirth": dob,
            "nationality": nation,
            "heightCm": None,
            "preferredFoot": None,
            "primaryPosition": map_position(row[pos_col] if pos_col else None),
            "fbrefId": player_fbref,
        }

        minutes = safe_int(row[min_col], 0) if min_col else 0
        apps = safe_int(row[mp_col], 0) if mp_col else 0
        if apps == 0 and minutes > 0:
            apps = 1
        if minutes > 0 and minutes < apps:
            minutes = apps

        seasons_rows.append(
            {
                "clubFbrefId": club_fbref,
                "playerFbrefId": player_fbref,
                "seasonLabel": label,
                "appearances": apps,
                "minutesPlayed": minutes,
                "goals": safe_int(row[gls_col], 0) if gls_col else 0,
                "assists": safe_int(row[ast_col], 0) if ast_col else 0,
                "xg": round(safe_float(row[xg_col], 0.0) if xg_col else 0.0, 2),
                "xa": round(safe_float(row[xa_col], 0.0) if xa_col else 0.0, 2),
                "primaryPosition": map_position(row[pos_col] if pos_col else None),
            }
        )

    return list(clubs.values()), list(players.values()), seasons_rows


def resolve_maps(api_url: str, dry_run: bool) -> tuple[dict[str, str], dict[str, str]]:
    if dry_run:
        return {}, {}
    clubs = {row["fbrefId"]: row["id"] for row in ensure_identity_page(api_url, "/api/v1/clubs") if row.get("fbrefId")}
    players = {
        row["fbrefId"]: row["id"] for row in ensure_identity_page(api_url, "/api/v1/players") if row.get("fbrefId")
    }
    return clubs, players


def main() -> int:
    args = parse_args()
    labels = [part.strip() for part in args.seasons.split(",") if part.strip()]
    league_ids = [part.strip() for part in args.leagues.split(",") if part.strip()]
    for league_id in league_ids:
        if league_id not in LEAGUES:
            raise SystemExit(f"Unsupported league {league_id!r}. Choose from: {', '.join(LEAGUES)}")

    print("Kleos FBref ingest")
    print(f"  leagues: {', '.join(league_ids)}")
    print(f"  seasons: {', '.join(labels)}")
    print(f"  api:     {args.api_url}{' (dry-run)' if args.dry_run else ''}")
    print("  policy:  docs/data-sourcing.md")

    tournament_ids = ensure_tournaments(args.api_url, args.dry_run, args.batch_size)
    season_ids = ensure_seasons(args.api_url, labels, args.dry_run, args.batch_size)

    all_clubs: dict[str, dict] = {}
    all_players: dict[str, dict] = {}
    pending_club_seasons: list[dict] = []
    pending_player_seasons: list[dict] = []
    counters = Counters()

    for league_id in league_ids:
        meta = LEAGUES[league_id]
        for label in labels:
            start_year = label_to_start_year(label)
            print(f"\nFetching {league_id} {label} (FBref season {start_year})…")
            standard, shooting = fetch_league_season(league_id, start_year)
            frame = merge_xg(standard, shooting)
            clubs, players, season_rows = build_rows_for_frame(frame, league_id, label, meta)
            for club in clubs:
                all_clubs[club["fbrefId"]] = club
            for player in players:
                all_players[player["fbrefId"]] = player

            tournament_id = tournament_ids[meta["tournament_name"]]
            season_id = season_ids[label]
            for row in season_rows:
                pending_club_seasons.append(
                    {
                        "clubFbrefId": row["clubFbrefId"],
                        "seasonId": season_id,
                        "tournamentId": tournament_id,
                    }
                )
                pending_player_seasons.append(row)

            counters.clubs = len(all_clubs)
            counters.players = len(all_players)
            print(f"  rows={len(season_rows)} clubs_total={counters.clubs} players_total={counters.players}")
            time.sleep(max(args.sleep, 0))

    print("\nUpserting clubs + players…")
    bulk_post(args.api_url, "/api/v1/clubs", list(all_clubs.values()), args.batch_size, args.dry_run)
    bulk_post(args.api_url, "/api/v1/players", list(all_players.values()), args.batch_size, args.dry_run)

    club_map, player_map = resolve_maps(args.api_url, args.dry_run)

    club_season_items = []
    seen_club_season = set()
    for row in pending_club_seasons:
        key = (row["clubFbrefId"], row["seasonId"])
        if key in seen_club_season:
            continue
        seen_club_season.add(key)
        club_id = club_map.get(row["clubFbrefId"], "dry-club")
        club_season_items.append(
            {
                "clubId": club_id,
                "seasonId": row["seasonId"],
                "tournamentId": row["tournamentId"],
            }
        )

    player_season_items = []
    skipped_unresolved = 0
    for row in pending_player_seasons:
        player_id = player_map.get(row["playerFbrefId"])
        club_id = club_map.get(row["clubFbrefId"])
        if args.dry_run:
            player_id = player_id or "dry-player"
            club_id = club_id or "dry-club"
        if not player_id or not club_id:
            skipped_unresolved += 1
            continue
        player_season_items.append(
            {
                "playerId": player_id,
                "clubId": club_id,
                "seasonId": season_ids[row["seasonLabel"]],
                "appearances": row["appearances"],
                "minutesPlayed": row["minutesPlayed"],
                "goals": row["goals"],
                "assists": row["assists"],
                "xg": row["xg"],
                "xa": row["xa"],
                "primaryPosition": row["primaryPosition"],
            }
        )
    if skipped_unresolved:
        print(f"  skipped {skipped_unresolved} player-season row(s) with unresolved player/club ids")

    print("\nUpserting club-seasons + player-seasons…")
    bulk_post(args.api_url, "/api/v1/club-seasons", club_season_items, args.batch_size, args.dry_run)
    bulk_post(args.api_url, "/api/v1/player-seasons", player_season_items, args.batch_size, args.dry_run)

    print(
        f"\nDone. unique_clubs={len(all_clubs)} unique_players={len(all_players)} "
        f"club_seasons={len(club_season_items)} player_seasons={len(player_season_items)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
