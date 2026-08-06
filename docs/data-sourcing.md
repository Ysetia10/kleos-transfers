# Data sourcing and licensing policy

Kleos Transfers is an open-source research/product project. Historical football data is loaded only from sources we can attribute and whose terms we can respect. This document is the checklist before any ingest work ships.

## Goals

- Load **Premier League** and **La Liga** identity + player-season history for seasons **2016/17 through 2025/26** (inclusive). Season **2026/27** is out of scope until it completes.
- Prefer **stable external IDs** (`fbrefId`) so a player/club is one Kleos row across seasons.
- Never commit scraped bulk datasets into git.

## Allowed sources (current)

| Source | Use for | Redistribution | Notes |
|--------|---------|----------------|-------|
| **FBref** (via [`soccerdata`](https://soccerdata.readthedocs.io/) or equivalent) | Clubs, players, PlayerSeason counting/expected stats | Do **not** republish raw FBref dumps in this repo | Primary ingest path (`scripts/ingest_fbref_pl_laliga.py`). Respect crawl delays; personal/research use only unless you obtain broader rights. |
| **Manual / first-party curated CSVs** | Small identity patches | OK if you created them | Use `scripts/import-identities.py`. |
| **StatsBomb Open Data** | Event research, not full PL/La Liga season coverage | Allowed under StatsBomb open-data terms | Not the PL/La Liga completeness path. |

## Not allowed without explicit license

- Redistributing Transfermarkt scrapes, Opta/StatsPerform dumps, or paid API extracts in this repository.
- Shipping someone else’s full historical database as “sample data”.
- Ignoring robots/rate limits or ToS of a scraped site.

## Attribution requirements

When publishing demos, papers, or public datasets derived from FBref:

1. Credit **FBref / Sports Reference** (and `soccerdata` if used).
2. State that Kleos stores a **derived, product-shaped subset** (identity + season aggregates), not a mirror of FBref pages.
3. Keep provenance in ingest logs (league, season, fetch timestamp).

## Uniqueness and identity rules for ingest

- **Player:** unique on `(fullNameNormalized, dateOfBirth, nationality)`; when present, `fbrefId` is also unique and is the preferred match key.
- **Club:** unique on `(nameNormalized, countryCode)`; when present, `fbrefId` is also unique.
- Soft delete frees those slots by suffixing `#<id>` (same pattern as elsewhere).
- Height and preferred foot are optional — season tables often omit them.

## Ingest checklist

Before running a full load against a shared database:

- [ ] Backend migrations applied (including player/club `fbref_id` uniqueness).
- [ ] You have read FBref / Sports Reference terms for your use case.
- [ ] Start with `--dry-run` and a single season (`--seasons 2024/25`).
- [ ] Confirm duplicate skips on a second run (idempotent).
- [ ] Do not commit `data/cache/` or downloaded CSVs.

## Demo seed

There is **no** fake demo-seed pipeline. Local UX should use the real ingest (possibly one season) or small hand-authored CSVs you own.
