#!/usr/bin/env python3
"""Infer COMPLETED transfers from consecutive PlayerSeason club changes.

Source: Kleos PlayerSeason history already ingested from FBref (derived, not a
Transfermarkt scrape). Status is always COMPLETED; fee is null; type is PERMANENT
unless notes suggest otherwise.

Rumours / announcements are out of scope for this script — import them via the
transfers bulk API with status=RUMOURED or ANNOUNCED.

Usage:
  python3 scripts/infer_transfers_from_seasons.py --api-url http://127.0.0.1:8080
  python3 scripts/infer_transfers_from_seasons.py --dry-run
"""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from collections import defaultdict


def get_json(url: str):
    with urllib.request.urlopen(url) as response:
        return json.load(response)


def post_json(url: str, payload: dict):
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req) as response:
        return json.load(response)


def fetch_all_pages(base: str, path: str, size: int = 200):
    page = 0
    while True:
        query = urllib.parse.urlencode({"page": page, "size": size, "sort": "id,asc"})
        payload = get_json(f"{base}{path}?{query}")
        content = payload.get("content") or []
        yield from content
        if payload.get("last", True) or not content:
            break
        page += 1


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--api-url", default="http://127.0.0.1:8080")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--limit", type=int, default=0, help="Max transfers to emit (0=all)")
    args = parser.parse_args()
    base = args.api_url.rstrip("/")

    seasons = {
        row["id"]: row
        for row in fetch_all_pages(base, "/api/v1/seasons")
    }
    season_order = sorted(
        seasons.values(),
        key=lambda row: row["startDate"],
    )
    season_rank = {row["id"]: idx for idx, row in enumerate(season_order)}

    by_player: dict[str, list[dict]] = defaultdict(list)
    for row in fetch_all_pages(base, "/api/v1/player-seasons", size=500):
        by_player[row["playerId"]].append(row)

    items: list[dict] = []
    for player_id, rows in by_player.items():
        rows = sorted(rows, key=lambda r: season_rank.get(r["seasonId"], 10_000))
        for prev, curr in zip(rows, rows[1:]):
            if prev["clubId"] == curr["clubId"]:
                continue
            # Only adjacent seasons in the global calendar
            if season_rank.get(curr["seasonId"], -1) - season_rank.get(prev["seasonId"], -2) != 1:
                continue
            to_season = seasons[curr["seasonId"]]
            items.append(
                {
                    "playerId": player_id,
                    "fromClubId": prev["clubId"],
                    "toClubId": curr["clubId"],
                    "seasonId": curr["seasonId"],
                    "transferDate": to_season["startDate"],
                    "feeEur": None,
                    "type": "PERMANENT",
                    "status": "COMPLETED",
                    "source": "kleos-player-season-diff",
                    "notes": f"Inferred from {prev.get('seasonLabel') or prev['seasonId']} → {to_season.get('label')}",
                }
            )

    if args.limit and args.limit > 0:
        items = items[: args.limit]

    print(f"Inferred {len(items)} completed club changes")
    if args.dry_run or not items:
        return 0

    created = 0
    skipped = 0
    batch_size = 200
    for offset in range(0, len(items), batch_size):
        batch = items[offset : offset + batch_size]
        try:
            result = post_json(f"{base}/api/v1/transfers/bulk", {"items": batch})
        except urllib.error.HTTPError as exc:
            body = exc.read().decode("utf-8", errors="replace")
            print(f"bulk failed: {exc.code} {body}", file=sys.stderr)
            return 1
        created += int(result.get("createdCount") or 0)
        skipped += int(result.get("skippedCount") or 0)
        print(f"batch {offset // batch_size + 1}: created={result.get('createdCount')} skipped={result.get('skippedCount')}")

    print(f"Done. created={created} skipped={skipped}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
