#!/usr/bin/env python3
"""Ensure forward predict-to Season identity rows exist (no PlayerSeason scrape).

Historical ingest stops at the last completed campaign (2025/26). Upcoming seasons
such as 2026/27 still need a Season row so the simulator can project as-of prior
context before outcomes exist.

Examples:
  ./scripts/ensure_predict_seasons.py
  ./scripts/ensure_predict_seasons.py --labels 2026/27 --dry-run
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from typing import Any


DEFAULT_API = os.environ.get("API_URL", "http://localhost:8080")
# (label, start_date, end_date) — July–June European campaign convention
DEFAULT_FORWARD = (("2026/27", "2026-07-01", "2027-06-30"),)


def http_json(method: str, url: str, body: dict[str, Any] | None = None) -> Any:
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"Accept": "application/json", "Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        raw = resp.read().decode("utf-8")
        return json.loads(raw) if raw else None


def list_season_labels(api: str) -> set[str]:
    labels: set[str] = set()
    page = 0
    while True:
        payload = http_json(
            "GET",
            f"{api.rstrip('/')}/api/v1/seasons?page={page}&size=50&sort=startDate,desc",
        )
        for row in payload.get("content") or []:
            labels.add(row["label"])
        if payload.get("last", True):
            break
        page += 1
    return labels


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-url", default=DEFAULT_API)
    parser.add_argument(
        "--labels",
        nargs="+",
        default=[label for label, _, _ in DEFAULT_FORWARD],
        help="Season labels to ensure (default: 2026/27)",
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    catalog = {label: (start, end) for label, start, end in DEFAULT_FORWARD}
    for label in args.labels:
        if label not in catalog:
            # Derive July–June window from YYYY/YY
            try:
                start_year = int(label.split("/")[0])
            except (ValueError, IndexError) as exc:
                print(f"Cannot derive dates for label {label!r}: {exc}", file=sys.stderr)
                return 2
            catalog[label] = (f"{start_year}-07-01", f"{start_year + 1}-06-30")

    try:
        existing = list_season_labels(args.api_url)
    except urllib.error.URLError as exc:
        print(f"API unreachable at {args.api_url}: {exc}", file=sys.stderr)
        return 1

    created = 0
    for label in args.labels:
        if label in existing:
            print(f"ok  {label} (already present)")
            continue
        start, end = catalog[label]
        body = {"label": label, "startDate": start, "endDate": end}
        if args.dry_run:
            print(f"dry {label} → {body}")
            continue
        http_json("POST", f"{args.api_url.rstrip('/')}/api/v1/seasons", body)
        print(f"add {label} ({start} → {end})")
        created += 1

    print(f"done created={created}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
