#!/usr/bin/env python3
"""Backfill precise pitch roles from the Premier League PulseLive API.

FBref season tables only expose GK/DF/MF/FW. Match-report scrapes are accurate but
throttled (~1 match/min). For the Premier League, footballapi.pulselive.com exposes
season-scoped squad staff with detailed ``positionInfo`` strings (e.g. Right Full Back)
in one request per club — much faster for ENG coverage.

Usage:
  python3 scripts/enrich_positions_from_premierleague.py --seasons 2024/25 --dry-run
  python3 scripts/enrich_positions_from_premierleague.py --seasons 2024/25 2025/26

Does not scrape Transfermarkt. Derived position updates only; credit Premier League /
PulseLive in docs when publishing.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.request
from collections import defaultdict

COMPETITION_ID = 1  # Premier League
API = "https://footballapi.pulselive.com/football"
HEADERS = {
    "Origin": "https://www.premierleague.com",
    "Referer": "https://www.premierleague.com/",
    "User-Agent": "KleosTransfersResearch/1.0 (personal research; contact via GitHub)",
    "Accept": "application/json",
}

# Labels that are too coarse for a reliable single slot — leave DB unchanged.
SKIP_LABELS = {
    "midfielder",
    "defender",
    "forward",
    "winger",
    "full back",
    "striker",
    "left/right winger",
    "left/centre/right winger",
    "left/centre/right second striker",
    "left/centre/right striker",
    "left/centre/right central defender",
    "left/centre/right central midfielder",
    "left/right wing back",
    "left/centre/right full back",
    "centre winger",
}

# Equivalent club-name keys (PulseLive long forms ↔ Kleos short forms).
CLUB_ALIAS_GROUPS: list[set[str]] = [
    {"brighton", "brightonandhovealbion", "brightonhovealbion"},
    {"manchesterunited", "manchesterutd"},
    {"newcastle", "newcastleunited", "newcastleutd"},
    {"nottingham", "nottinghamforest", "nottmforest", "nottforest"},
    {"tottenham", "tottenhamhotspur"},
    {"westham", "westhamunited"},
    {"wolves", "wolverhamptonwanderers"},
    {"leicester", "leicestercity"},
    {"ipswich", "ipswichtown"},
]


def normalize_name(value: str) -> str:
    folded = unicodedata.normalize("NFKD", value)
    ascii_only = "".join(ch for ch in folded if not unicodedata.combining(ch))
    return re.sub(r"[^a-z0-9]+", "", ascii_only.lower())


def map_position_info(raw: str | None) -> str | None:
    if not raw:
        return None
    text = re.sub(r"\s+", " ", raw.strip())
    key = text.lower()
    if key in SKIP_LABELS:
        return None

    # Ordered specific → general.
    rules: list[tuple[str, str]] = [
        ("goalkeeper", "GK"),
        ("right wing back", "RWB"),
        ("left wing back", "LWB"),
        ("right full back", "RB"),
        ("left full back", "LB"),
        ("centre/right full back", "RB"),
        ("left/centre full back", "LB"),
        ("centre defensive midfielder", "CDM"),
        ("defensive midfielder", "CDM"),
        ("centre attacking midfielder", "CAM"),
        ("left/centre attacking midfielder", "CAM"),
        ("centre/right attacking midfielder", "CAM"),
        ("left/centre/right attacking midfielder", "CAM"),
        ("left/right attacking midfielder", "CAM"),
        ("left attacking midfielder", "CAM"),
        ("attacking midfielder", "CAM"),
        ("right winger", "RW"),
        ("left winger", "LW"),
        ("left/centre winger", "LW"),
        ("centre second striker", "CF"),
        ("left/centre second striker", "CF"),
        ("second striker", "CF"),
        ("centre striker", "ST"),
        ("left/centre striker", "ST"),
        ("centre central midfielder", "CM"),
        ("centre/right central midfielder", "CM"),
        ("left central midfielder", "CM"),
        ("central midfielder", "CM"),
        ("centre central defender", "CB"),
        ("left/centre central defender", "CB"),
        ("centre/right central defender", "CB"),
        ("left central defender", "CB"),
        ("central defender", "CB"),
    ]
    for needle, code in rules:
        if key == needle or key.endswith(needle):
            return code
    return None


def club_aliases(name: str) -> set[str]:
    base = normalize_name(name)
    aliases = {base}
    for group in CLUB_ALIAS_GROUPS:
        if base in group:
            aliases |= group
    return aliases


def player_name_keys(value: str) -> set[str]:
    """Match PulseLive display names to Kleos full names (accents / hyphen variants)."""
    full = normalize_name(value)
    keys = {full}
    # Drop hyphen so Wan-Bissaka ↔ Wan Bissaka collapses the same way after normalize.
    keys.add(full.replace("-", ""))
    parts = [p for p in re.split(r"\s+", value.strip()) if p]
    if len(parts) >= 2:
        keys.add(normalize_name(parts[0] + parts[-1]))
    return {k for k in keys if k}

def connect(url: str):
    try:
        import psycopg
    except ImportError:
        try:
            import psycopg2 as psycopg  # type: ignore
        except ImportError as exc:
            raise SystemExit("Install psycopg: pip install 'psycopg[binary]'") from exc
    return psycopg.connect(url)


def http_json(url: str, *, retries: int = 5) -> dict:
    last_err: Exception | None = None
    for attempt in range(retries):
        req = urllib.request.Request(url, headers=HEADERS)
        try:
            with urllib.request.urlopen(req, timeout=45) as resp:
                body = resp.read().decode("utf-8").strip()
                if not body:
                    raise ValueError("empty response body")
                return json.loads(body)
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")[:300]
            if error.code in {429, 500, 502, 503, 504} and attempt + 1 < retries:
                wait = 2.0 * (attempt + 1)
                print(f"  retry HTTP {error.code} in {wait:.0f}s ({url})")
                time.sleep(wait)
                last_err = error
                continue
            raise SystemExit(f"GET {url} -> HTTP {error.code}: {detail}") from error
        except (urllib.error.URLError, TimeoutError, ValueError, json.JSONDecodeError) as error:
            if attempt + 1 < retries:
                wait = 2.0 * (attempt + 1)
                print(f"  retry {type(error).__name__} in {wait:.0f}s ({url})")
                time.sleep(wait)
                last_err = error
                continue
            raise SystemExit(f"GET {url} failed after {retries} attempts: {error}") from error
    raise SystemExit(f"GET {url} failed: {last_err}")


def resolve_comp_season_id(season_label: str) -> int:
    # PulseLive labels are "2024/25" or "English Premier League Season 2026/2027".
    wanted = season_label.strip()
    wanted_alt = wanted.replace("/", "/20") if re.fullmatch(r"\d{4}/\d{2}", wanted) else wanted
    payload = http_json(f"{API}/competitions/{COMPETITION_ID}/compseasons?page=0&pageSize=40")
    for row in payload.get("content") or []:
        label = str(row.get("label") or "")
        if label == wanted or label.endswith(wanted) or wanted in label:
            return int(row["id"])
        if wanted_alt and wanted_alt in label:
            return int(row["id"])
    raise SystemExit(f"No Premier League compSeason for label {season_label!r}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--database-url",
        default=os.environ.get(
            "KLEOS_DATABASE",
            os.environ.get("DATABASE_URL", "postgresql://kleos:kleos@localhost:5432/kleos_transfers"),
        ),
    )
    parser.add_argument("--seasons", nargs="+", default=["2024/25", "2025/26"])
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--sleep", type=float, default=0.35)
    args = parser.parse_args()
    if str(args.database_url).startswith("jdbc:"):
        raise SystemExit("Pass a postgresql:// URL via --database-url (not JDBC)")

    season_updates = 0
    player_updates = 0
    skipped_coarse = 0
    matched = 0

    with connect(args.database_url) as conn:
        for season_label in args.seasons:
            comp_season_id = resolve_comp_season_id(season_label)
            print(f"Premier League {season_label} (compSeason={comp_season_id})")
            teams = http_json(
                f"{API}/teams?page=0&pageSize=25&compSeasons={comp_season_id}"
            ).get("content") or []

            season_matched = 0
            season_skipped = 0
            season_changed = 0

            # club_key -> [(ps_id, player_id, player_name, old_pos)]
            with conn.cursor() as cur:
                cur.execute(
                    """
                    SELECT ps.id, p.id, p.full_name, c.name, ps.primary_position
                    FROM player_seasons ps
                    JOIN players p ON p.id = ps.player_id AND p.deleted_at IS NULL
                    JOIN clubs c ON c.id = ps.club_id AND c.deleted_at IS NULL
                    JOIN seasons s ON s.id = ps.season_id AND s.deleted_at IS NULL
                    WHERE ps.deleted_at IS NULL
                      AND s.label = %s
                      AND c.country_code = 'ENG'
                    """,
                    (season_label,),
                )
                targets = cur.fetchall()

            by_club: dict[str, list[tuple]] = defaultdict(list)
            for row in targets:
                for alias in club_aliases(row[3]):
                    by_club[alias].append(row)

            for team in teams:
                team_id = int(team["id"])
                club_name = (team.get("club") or {}).get("name") or team.get("name") or ""
                staff = http_json(
                    f"{API}/teams/{team_id}/compseasons/{comp_season_id}/staff?pageSize=100"
                )
                time.sleep(args.sleep)
                club_keys = club_aliases(club_name)
                candidates: list[tuple] = []
                for key in club_keys:
                    candidates.extend(by_club.get(key) or [])
                # de-dupe by ps id
                seen_ps: set = set()
                uniq_candidates = []
                for row in candidates:
                    if row[0] in seen_ps:
                        continue
                    seen_ps.add(row[0])
                    uniq_candidates.append(row)

                # Index candidates by all name keys for O(1) lookup.
                by_player: dict[str, list[tuple]] = defaultdict(list)
                for row in uniq_candidates:
                    for key in player_name_keys(row[2]):
                        by_player[key].append(row)

                for player in staff.get("players") or []:
                    info = player.get("info") or {}
                    mapped = map_position_info(info.get("positionInfo"))
                    if mapped is None:
                        season_skipped += 1
                        skipped_coarse += 1
                        continue
                    display = ((player.get("name") or {}).get("display") or "").strip()
                    if not display:
                        continue
                    hit = None
                    for key in player_name_keys(display):
                        rows = by_player.get(key) or []
                        if len(rows) == 1:
                            hit = rows[0]
                            break
                        if len(rows) > 1:
                            # Prefer exact full-name normalize equality.
                            exact = [r for r in rows if normalize_name(r[2]) == normalize_name(display)]
                            hit = exact[0] if exact else rows[0]
                            break
                    if hit is None:
                        continue
                    season_matched += 1
                    matched += 1
                    ps_id, player_id, _name, _club, old_pos = hit
                    if old_pos == mapped:
                        continue
                    season_changed += 1
                    if args.dry_run:
                        season_updates += 1
                        continue
                    with conn.cursor() as cur:
                        cur.execute(
                            """
                            UPDATE player_seasons
                            SET primary_position = %s, updated_at = NOW()
                            WHERE id = %s
                            """,
                            (mapped, ps_id),
                        )
                    season_updates += 1

            if not args.dry_run:
                with conn.cursor() as cur:
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
                            JOIN clubs c ON c.id = ps.club_id
                            WHERE ps.deleted_at IS NULL
                              AND s.label = %s
                              AND c.country_code = 'ENG'
                              AND ps.primary_position IS NOT NULL
                            ORDER BY ps.player_id, ps.minutes_played DESC NULLS LAST
                        ) src
                        WHERE p.id = src.player_id
                          AND p.deleted_at IS NULL
                          AND p.primary_position IS DISTINCT FROM src.primary_position
                        """,
                        (season_label,),
                    )
                    player_updates += cur.rowcount
                conn.commit()

            print(
                f"  teams={len(teams)} db_rows={len(targets)} "
                f"name_matches={season_matched} changed={season_changed} "
                f"skipped_coarse={season_skipped}"
            )

    print(
        f"Done. season_updates≈{season_updates} player_updates≈{player_updates} "
        f"dry_run={args.dry_run}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
