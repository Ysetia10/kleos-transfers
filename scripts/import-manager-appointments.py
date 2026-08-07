#!/usr/bin/env python3
"""Import manager identities and club appointments from a CSV.

CSV columns:
  clubName,managerFullName,dateOfBirth,nationality,seasonLabel

Usage:
  scripts/import-manager-appointments.py scripts/sample-data/manager-appointments-2025-26.csv
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
    parser.add_argument("csv_file", help="path to appointments CSV")
    parser.add_argument("--api-url", default="http://localhost:8080", help="backend base URL")
    return parser.parse_args()


def api_json(api_url: str, method: str, path: str, payload: dict | None = None, allow_conflict: bool = False):
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
        if allow_conflict and error.code in {409, 400}:
            return None
        raise SystemExit(f"{method} {path} failed with HTTP {error.code}: {detail}") from error
    except OSError as error:
        raise SystemExit(f"Could not reach {api_url}: {error}") from error


def list_all(api_url: str, path: str, key_name: str | None = None) -> list[dict]:
    items: list[dict] = []
    page = 0
    while True:
        query = urllib.parse.urlencode({"page": page, "size": 200})
        payload = api_json(api_url, "GET", f"{path}?{query}")
        content = payload.get("content", [])
        items.extend(content)
        if payload.get("last", True) or not content:
            break
        page += 1
    if key_name:
        return items
    return items


def index_by_name(items: list[dict], field: str) -> dict[str, dict]:
    return {item[field].strip().casefold(): item for item in items}


def main() -> None:
    args = parse_args()
    with open(args.csv_file, newline="", encoding="utf-8-sig") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise SystemExit(f"{args.csv_file} contains no data rows")

    clubs = index_by_name(list_all(args.api_url, "/api/v1/clubs"), "name")
    seasons = index_by_name(list_all(args.api_url, "/api/v1/seasons"), "label")
    managers = index_by_name(list_all(args.api_url, "/api/v1/managers"), "fullName")

    created_managers = 0
    created_appointments = 0
    skipped = 0

    for index, row in enumerate(rows, start=2):
        club_name = (row.get("clubName") or "").strip()
        manager_name = (row.get("managerFullName") or "").strip()
        dob = (row.get("dateOfBirth") or "").strip()
        nationality = (row.get("nationality") or "").strip().upper()
        season_label = (row.get("seasonLabel") or "").strip()

        club = clubs.get(club_name.casefold())
        season = seasons.get(season_label.casefold())
        if club is None:
            print(f"  skip line {index}: unknown club '{club_name}'")
            skipped += 1
            continue
        if season is None:
            print(f"  skip line {index}: unknown season '{season_label}'")
            skipped += 1
            continue

        manager = managers.get(manager_name.casefold())
        if manager is None:
            created = api_json(
                args.api_url,
                "POST",
                "/api/v1/managers",
                {
                    "fullName": manager_name,
                    "dateOfBirth": dob,
                    "nationality": nationality,
                },
                allow_conflict=True,
            )
            if created is None:
                managers = index_by_name(list_all(args.api_url, "/api/v1/managers"), "fullName")
                manager = managers.get(manager_name.casefold())
                if manager is None:
                    print(f"  skip line {index}: could not create/find manager '{manager_name}'")
                    skipped += 1
                    continue
            else:
                manager = created
                managers[manager_name.casefold()] = manager
                created_managers += 1

        created_appointment = api_json(
            args.api_url,
            "POST",
            "/api/v1/manager-seasons",
            {
                "managerId": manager["id"],
                "clubId": club["id"],
                "seasonId": season["id"],
            },
            allow_conflict=True,
        )
        if created_appointment is None:
            print(f"  exists {manager_name} -> {club_name} ({season_label})")
            skipped += 1
            continue
        created_appointments += 1
        print(f"  linked {manager_name} -> {club_name} ({season_label})")

    print(
        f"Done. managers_created={created_managers} "
        f"appointments_created={created_appointments} skipped={skipped}"
    )


if __name__ == "__main__":
    main()
