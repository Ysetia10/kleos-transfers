#!/usr/bin/env python3
"""Create stub Player identities for unmatched Wikipedia transfer names (top-5 only).

Summer-window wiki lists include many EFL / foreign lower-league movers we do not track.
This script only creates stubs when the move touches a club that already has a Big-5
ClubSeason (PL / La Liga / Bundesliga / Serie A / Ligue 1) in 2025/26 or 2026/27.

Stubs use YEAR birth precision (1999-07-01) and the destination/source club's country
as a provisional nationality so transfers can attach; enrich bio later.

Usage:
  python3 scripts/ensure_wiki_transfer_players.py --dry-run
  python3 scripts/ensure_wiki_transfer_players.py --pages top5-summer-2026
  # then: python3 scripts/ingest_transfers_from_wikipedia.py --pages top5-summer-2026
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

# Reuse wiki helpers.
ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

DEFAULT_DB = "postgresql://kleos:kleos@localhost:5432/kleos_transfers"
DEFAULT_API = os.environ.get("API_URL", "http://localhost:8080")
TOP5_TOURNAMENTS = (
    "Premier League",
    "La Liga",
    "Bundesliga",
    "Serie A",
    "Ligue 1",
)


def http_json(method: str, url: str, body: dict | None = None):
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=90) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else None


def load_top5_clubs(db: str) -> dict[str, tuple[str, str]]:
    """name_lower -> (country_code, club_id)."""
    labels = "','".join(TOP5_TOURNAMENTS)
    sql = f"""
    SELECT COALESCE(json_agg(row_to_json(q)), '[]'::json) FROM (
      SELECT DISTINCT lower(c.name) AS name, c.country_code, c.id::text AS id
      FROM clubs c
      JOIN club_seasons cs ON cs.club_id = c.id AND cs.deleted_at IS NULL
      JOIN tournaments t ON t.id = cs.tournament_id
      JOIN seasons s ON s.id = cs.season_id
      WHERE c.deleted_at IS NULL
        AND s.label IN ('2025/26', '2026/27')
        AND t.name IN ('{labels}')
    ) q
    """
    out = subprocess.check_output(
        ["psql", db, "-v", "ON_ERROR_STOP=1", "-t", "-A", "-c", sql], text=True
    ).strip()
    rows = json.loads(out or "[]")
    return {r["name"]: (r["country_code"], r["id"]) for r in rows}


def main() -> int:
    # Import after path setup.
    import ingest_transfers_from_wikipedia as wiki  # type: ignore

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-url", default=DEFAULT_API)
    parser.add_argument("--db", default=os.environ.get("KLEOS_DATABASE", DEFAULT_DB))
    parser.add_argument("--pages", default="top5-summer-2026")
    parser.add_argument("--sleep", type=float, default=0.8)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0)
    args = parser.parse_args()

    if args.pages in wiki.PAGE_PACKS:
        titles = wiki.PAGE_PACKS[args.pages]
    else:
        titles = [t.strip() for t in args.pages.split(",") if t.strip()]

    top5 = load_top5_clubs(args.db)
    id_to_cc = {cid: cc for _, (cc, cid) in top5.items()}
    print(f"top-5 clubs in scope: {len(top5)}")

    player_index = wiki.build_player_index(args.api_url)
    club_index = wiki.build_club_index(args.api_url)

    def top5_country(club_name: str | None) -> str | None:
        if not club_name:
            return None
        cid = wiki.search_club(club_name, club_index)
        if cid and cid in id_to_cc:
            return id_to_cc[cid]
        q = wiki.normalize_club_query(club_name).lower()
        if q in top5:
            return top5[q][0]
        folded = wiki.fold_name(club_name)
        for name, (cc, _) in top5.items():
            if wiki.fold_name(name) == folded:
                return cc
        return None

    raw: list = []
    for title in titles:
        print(f"fetch {title}")
        wt = wiki.wiki_wikitext(title)
        parsed = wiki.parse_permanent_transfers(title, wt)
        print(f"  parsed {len(parsed)}")
        raw.extend(parsed)
        time.sleep(args.sleep)

    club_names = {wiki.fold_name(name) for name in top5}
    # Also block any Kleos club name (EFL etc. that appear as "player" cells in wiki tables).
    all_clubs_json = subprocess.check_output(
        [
            "psql",
            args.db,
            "-v",
            "ON_ERROR_STOP=1",
            "-t",
            "-A",
            "-c",
            "SELECT COALESCE(json_agg(name), '[]'::json) FROM clubs WHERE deleted_at IS NULL",
        ],
        text=True,
    ).strip()
    for name in json.loads(all_clubs_json or "[]"):
        club_names.add(wiki.fold_name(name))

    stubs: list[dict] = []
    seen: set[str] = set()
    skipped_club_cell = 0
    for row in raw:
        if wiki.search_player(row.player, player_index):
            continue
        key = row.player.lower().strip()
        if key in seen:
            continue
        folded_player = wiki.fold_name(row.player)
        if folded_player in club_names or wiki.search_club(row.player, club_index):
            skipped_club_cell += 1
            continue
        # Reject mononyms / empty / obvious non-person tokens.
        tokens = [t for t in folded_player.replace("-", " ").split() if t]
        if len(tokens) < 2:
            skipped_club_cell += 1
            continue
        cc = top5_country(row.to_club) or top5_country(row.from_club)
        if not cc:
            continue
        seen.add(key)
        stubs.append(
            {
                "fullName": row.player.strip(),
                "dateOfBirth": "1999-07-01",
                "dateOfBirthPrecision": "YEAR",
                "nationality": cc,
                "primaryPosition": "CM",
            }
        )

    if args.limit > 0:
        stubs = stubs[: args.limit]

    print(f"stub players to create: {len(stubs)} (skipped club/mononym cells={skipped_club_cell})")
    for item in stubs[:15]:
        print(f"  {item['fullName']} ({item['nationality']})")
    if len(stubs) > 15:
        print(f"  … +{len(stubs) - 15} more")

    if args.dry_run or not stubs:
        return 0

    created = skipped = 0
    for i in range(0, len(stubs), 100):
        chunk = stubs[i : i + 100]
        try:
            resp = http_json(
                "POST",
                f"{args.api_url.rstrip('/')}/api/v1/players/bulk",
                {"items": chunk},
            )
        except urllib.error.HTTPError as exc:
            print(exc.read().decode("utf-8", errors="replace"), file=sys.stderr)
            return 1
        created += int(resp.get("createdCount") or 0)
        skipped += int(resp.get("skippedCount") or 0)
        print(f"  bulk {i // 100 + 1}: created={resp.get('createdCount')} skipped={resp.get('skippedCount')}")

    print(f"Done. created={created} skipped={skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
