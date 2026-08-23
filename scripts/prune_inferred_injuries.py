#!/usr/bin/env python3
"""Soft-delete inferred injury spells when a confirmed spell overlaps for the same player.

Overlap window: inferred spell intersects confirmed ±45 days.

Usage:
  python3 scripts/prune_inferred_injuries.py --dry-run
  python3 scripts/prune_inferred_injuries.py --apply
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

DEFAULT_API = "http://localhost:8080"
INFERRED_TYPE = "Inferred availability gap"
OVERLAP_PAD_DAYS = 45


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--api-url", default=DEFAULT_API)
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--apply", action="store_true")
    return parser.parse_args()


def api_json(api_url: str, method: str, path: str, payload: dict | None = None):
    data = None if payload is None else json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        f"{api_url.rstrip('/')}{path}",
        data=data,
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        method=method,
    )
    with urllib.request.urlopen(request) as response:
        body = response.read()
        return json.loads(body) if body else None


def parse_date(value: str | None) -> date | None:
    if not value:
        return None
    return date.fromisoformat(value)


def overlaps(
    inferred_start: date,
    inferred_end: date | None,
    confirmed_start: date,
    confirmed_end: date | None,
) -> bool:
    inferred_end_eff = inferred_end or date.today()
    confirmed_end_eff = confirmed_end or date.today()
    pad_start = confirmed_start - timedelta(days=OVERLAP_PAD_DAYS)
    pad_end = confirmed_end_eff + timedelta(days=OVERLAP_PAD_DAYS)
    return inferred_start <= pad_end and inferred_end_eff >= pad_start


def list_injuries(api_url: str) -> list[dict]:
    items: list[dict] = []
    page = 0
    while True:
        payload = api_json(api_url, "GET", f"/api/v1/injuries?page={page}&size=200")
        content = payload.get("content", [])
        items.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
    return items


def main() -> None:
    args = parse_args()
    if not args.dry_run and not args.apply:
        raise SystemExit("Pass --dry-run or --apply")

    injuries = list_injuries(args.api_url)
    inferred = [row for row in injuries if row.get("injuryType") == INFERRED_TYPE]
    confirmed = [row for row in injuries if row.get("injuryType") != INFERRED_TYPE]

    to_delete: list[dict] = []
    confirmed_player_ids = {row["playerId"] for row in confirmed}
    for inferred_row in inferred:
        if inferred_row["playerId"] in confirmed_player_ids:
            to_delete.append(inferred_row)
            continue
        player_id = inferred_row["playerId"]
        i_start = parse_date(inferred_row["startDate"])
        i_end = parse_date(inferred_row.get("endDate"))
        if i_start is None:
            continue
        player_confirmed = [row for row in confirmed if row["playerId"] == player_id]
        for conf in player_confirmed:
            c_start = parse_date(conf["startDate"])
            if c_start is None:
                continue
            c_end = parse_date(conf.get("endDate"))
            if overlaps(i_start, i_end, c_start, c_end):
                to_delete.append(inferred_row)
                break

    print(f"Found {len(to_delete)} inferred spell(s) overlapping confirmed coverage")
    for row in to_delete:
        print(f"  {row['playerName']}: {row['startDate']} → {row.get('endDate')} ({row['id']})")

    if args.dry_run:
        return

    for row in to_delete:
        api_json(args.api_url, "DELETE", f"/api/v1/injuries/{row['id']}")
    print(f"Deleted {len(to_delete)} inferred spell(s)")


if __name__ == "__main__":
    main()
