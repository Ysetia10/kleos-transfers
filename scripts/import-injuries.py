#!/usr/bin/env python3
"""Import injury spells from CSV into POST /api/v1/injuries/bulk.

CSV columns:
  playerFullName,injuryType,severity,startDate,endDate[,playerId]

severity: MINOR | MODERATE | SEVERE
endDate: optional (blank = ongoing)

Prefer playerId when present; otherwise resolve by exact fullName.

Usage:
  scripts/import-injuries.py scripts/sample-data/injuries.csv
"""

from __future__ import annotations

import argparse
import csv
import json
import sys
import urllib.error
import urllib.parse
import urllib.request


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("csv_file", help="path to injuries CSV")
    parser.add_argument("--api-url", default="http://localhost:8080")
    parser.add_argument("--batch-size", type=int, default=200)
    return parser.parse_args()


def api_json(api_url: str, method: str, path: str, payload: dict | None = None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        f"{api_url.rstrip('/')}{path}",
        data=data,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method=method,
    )
    try:
        with urllib.request.urlopen(request) as response:
            body = response.read()
            return json.loads(body) if body else None
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"{method} {path} failed with HTTP {error.code}: {detail}") from error
    except OSError as error:
        raise SystemExit(f"Could not reach {api_url}: {error}") from error


def list_players(api_url: str) -> dict[str, dict]:
    items: list[dict] = []
    page = 0
    while True:
        query = urllib.parse.urlencode({"page": page, "size": 200})
        payload = api_json(api_url, "GET", f"/api/v1/players?{query}")
        content = payload.get("content", [])
        items.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
    return {p["fullName"].strip().casefold(): p for p in items}


def main() -> None:
    args = parse_args()
    with open(args.csv_file, newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise SystemExit(f"{args.csv_file} contains no data rows")

    players = list_players(args.api_url)
    items: list[dict] = []
    skipped = 0
    for index, row in enumerate(rows, start=2):
        player_id = (row.get("playerId") or "").strip()
        name = (row.get("playerFullName") or "").strip()
        injury_type = (row.get("injuryType") or "").strip()
        severity = (row.get("severity") or "").strip().upper()
        start = (row.get("startDate") or "").strip()
        end = (row.get("endDate") or "").strip() or None

        if not player_id:
            player = players.get(name.casefold())
            if player is None:
                print(f"  skip line {index}: unknown player '{name}'")
                skipped += 1
                continue
            player_id = player["id"]

        if not injury_type or not severity or not start:
            print(f"  skip line {index}: missing injuryType/severity/startDate")
            skipped += 1
            continue

        items.append(
            {
                "playerId": player_id,
                "injuryType": injury_type,
                "severity": severity,
                "startDate": start,
                "endDate": end,
            }
        )

    print(f"Prepared {len(items)} injuries ({skipped} skipped)")
    for i in range(0, len(items), args.batch_size):
        chunk = items[i : i + args.batch_size]
        result = api_json(args.api_url, "POST", "/api/v1/injuries/bulk", {"items": chunk})
        print(f"  batch {i // args.batch_size + 1}: {result}")


if __name__ == "__main__":
    main()
