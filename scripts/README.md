# Scripts

Small, documented automation for development and maintenance. Scripts must not embed secrets or unlicensed data dumps.

## `create-roadmap-issues.sh`

Idempotent helper that creates GitHub labels, milestones, and roadmap issues.

```bash
./scripts/create-roadmap-issues.sh
```

Requires `gh` authenticated (`gh auth login`).

## `import-identities.py`

Loads small identity CSVs you own into a running backend through the bulk API. Python 3 standard library only.

```bash
./scripts/import-identities.py players scripts/sample-data/players.csv
./scripts/import-identities.py clubs scripts/sample-data/clubs.csv
```

Options: `--api-url`, `--batch-size`, `--dry-run`.

Player columns: `fullName,dateOfBirth,nationality,primaryPosition` plus optional `heightCm,preferredFoot,fbrefId`.  
Club columns: `name,shortName,countryCode` plus optional `foundedYear,fbrefId`.

## `ingest_fbref_pl_laliga.py`

**Primary historical load** for Premier League + La Liga, seasons **2016/17–2025/26**.

Reads FBref via `soccerdata`, then upserts tournaments, seasons, clubs, players, club-seasons, and player-seasons through the Kleos bulk APIs. Player/club matching uses `fbrefId` so re-runs are idempotent.

```bash
pip install -r scripts/requirements-ingest.txt
# backend must be running
./scripts/ingest_fbref_pl_laliga.py --dry-run --seasons 2024/25
./scripts/ingest_fbref_pl_laliga.py --seasons 2024/25
./scripts/ingest_fbref_pl_laliga.py   # full 2016/17 … 2025/26 window
```

Policy and attribution: [`docs/data-sourcing.md`](../docs/data-sourcing.md).

Do not commit downloaded caches or bulk CSVs (`data/` is gitignored).
