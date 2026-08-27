#!/usr/bin/env python3
"""Import identity records from a CSV file into the Kleos Transfers API.

Usage:
    scripts/import-identities.py players scripts/sample-data/players.csv
    scripts/import-identities.py clubs data/clubs.csv --api-url http://localhost:8080

The CSV header must use the API field names for the resource (see RESOURCES below).
Rows are sent to POST /api/v1/<resource>/bulk in batches; the API reports rows that
were skipped as duplicates or rejected as invalid, and this script prints them with
their CSV line number.

Requires only the Python 3 standard library.
"""

import argparse
import csv
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
if str(SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_DIR))

from kleos_api import auth_headers

MAX_BATCH_SIZE = 500

RESOURCES = {
    "players": {
        "required": ["fullName", "dateOfBirth", "nationality", "primaryPosition"],
        "optional": ["heightCm", "preferredFoot", "fbrefId", "dateOfBirthPrecision"],
        "integers": ["heightCm"],
    },
    "clubs": {
        "required": ["name", "shortName", "countryCode"],
        "optional": ["foundedYear", "fbrefId"],
        "integers": ["foundedYear"],
    },
    "managers": {
        "required": ["fullName", "dateOfBirth", "nationality"],
        "optional": [],
        "integers": [],
    },
    "seasons": {
        "required": ["label", "startDate", "endDate"],
        "optional": [],
        "integers": [],
    },
    "tournaments": {
        "required": ["name", "shortName", "confederation", "type"],
        "optional": ["countryCode"],
        "integers": [],
    },
}


def parse_arguments():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("resource", choices=sorted(RESOURCES), help="identity type to import")
    parser.add_argument("csv_file", help="path to the CSV file")
    parser.add_argument("--api-url", default="http://localhost:8080", help="backend base URL")
    parser.add_argument("--batch-size", type=int, default=200, help=f"rows per request (max {MAX_BATCH_SIZE})")
    parser.add_argument("--dry-run", action="store_true", help="parse the CSV and print the payload without sending")
    arguments = parser.parse_args()
    if not 1 <= arguments.batch_size <= MAX_BATCH_SIZE:
        parser.error(f"--batch-size must be between 1 and {MAX_BATCH_SIZE}")
    return arguments


def read_rows(csv_path, spec):
    """Reads the CSV into API items, keeping each item's CSV line number."""
    with open(csv_path, newline="", encoding="utf-8-sig") as csv_file:
        reader = csv.DictReader(csv_file)
        known_fields = spec["required"] + spec["optional"]
        missing = [field for field in spec["required"] if field not in (reader.fieldnames or [])]
        if missing:
            raise SystemExit(f"{csv_path} is missing required column(s): {', '.join(missing)}")

        items = []
        for line_number, row in enumerate(reader, start=2):
            item = {}
            for field in known_fields:
                value = (row.get(field) or "").strip()
                if not value:
                    continue
                item[field] = int(value) if field in spec["integers"] else value
            items.append((line_number, item))
        return items


def post_batch(api_url, resource, items):
    payload = json.dumps({"items": items}).encode("utf-8")
    request = urllib.request.Request(
        f"{api_url.rstrip('/')}/api/v1/{resource}/bulk",
        data=payload,
        headers=auth_headers(),
        method="POST",
    )
    try:
        with urllib.request.urlopen(request) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise SystemExit(f"Request failed with HTTP {error.code}: {body}") from error
    except OSError as error:
        # Covers URLError plus connections dropped mid-request.
        raise SystemExit(f"Could not reach {api_url}: {error}") from error


def report_issues(label, issues, line_numbers):
    for issue in issues:
        line = line_numbers[issue["index"]]
        print(f"  {label} (line {line}): {issue['reference']} - {issue['reason']}")


def main():
    arguments = parse_arguments()
    spec = RESOURCES[arguments.resource]
    rows = read_rows(arguments.csv_file, spec)
    if not rows:
        raise SystemExit(f"{arguments.csv_file} contains no data rows")

    if arguments.dry_run:
        print(json.dumps({"items": [item for _, item in rows]}, indent=2))
        return 0

    totals = {"created": 0, "skipped": 0, "failed": 0}
    for start in range(0, len(rows), arguments.batch_size):
        batch = rows[start:start + arguments.batch_size]
        line_numbers = [line for line, _ in batch]
        result = post_batch(arguments.api_url, arguments.resource, [item for _, item in batch])

        totals["created"] += result["createdCount"]
        totals["skipped"] += result["skippedCount"]
        totals["failed"] += result["failedCount"]

        print(f"Batch {start + 1}-{start + len(batch)}: "
              f"{result['createdCount']} created, "
              f"{result['skippedCount']} skipped, "
              f"{result['failedCount']} failed")
        report_issues("skipped", result["skipped"], line_numbers)
        report_issues("failed", result["failed"], line_numbers)

    print(f"\nDone: {totals['created']} created, {totals['skipped']} skipped, {totals['failed']} failed "
          f"out of {len(rows)} rows.")
    return 1 if totals["failed"] else 0


if __name__ == "__main__":
    sys.exit(main())
