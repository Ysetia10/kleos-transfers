# Scripts

Small, documented automation for development and maintenance. Scripts must not embed secrets or unlicensed data.

## `create-roadmap-issues.sh`

Idempotent helper that creates GitHub labels, milestones, and roadmap issues for Kleos Transfers.

```bash
./scripts/create-roadmap-issues.sh
```

Requires `gh` authenticated (`gh auth login`).

## `import-identities.py`

Loads identity records from a CSV into a running backend through the bulk API. Python 3 standard library only.

```bash
# Start the backend first, then:
./scripts/import-identities.py players scripts/sample-data/players.csv
./scripts/import-identities.py clubs scripts/sample-data/clubs.csv
./scripts/import-identities.py managers scripts/sample-data/managers.csv
./scripts/import-identities.py seasons scripts/sample-data/seasons.csv
```

Options: `--api-url` (default `http://localhost:8080`), `--batch-size` (default 200, max 500), and `--dry-run` to print the payload without sending it.

The CSV header must use API field names:

| Resource | Columns |
|----------|---------|
| `players` | `fullName,dateOfBirth,nationality,heightCm,preferredFoot,primaryPosition` |
| `clubs` | `name,shortName,countryCode` and optional `foundedYear` |
| `managers` | `fullName,dateOfBirth,nationality` |
| `seasons` | `label,startDate,endDate` |

Re-running an import is safe: rows that already exist are reported as skipped rather than duplicated. Rows that fail validation are reported with their CSV line number and do not block the rest of the file.
