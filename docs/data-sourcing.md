# Data sourcing and licensing policy

Kleos Transfers is an open-source research/product project. Historical football data is loaded only from sources we can attribute and whose terms we can respect. This document is the checklist before any ingest work ships.

## Goals

- Load the **top five European leagues** (Premier League, La Liga, Bundesliga, Serie A, Ligue 1) identity + player-season history for seasons **2016/17 through 2025/26** (inclusive). Season **2026/27** is out of scope until it completes.
- Prefer **stable external IDs** (`fbrefId`) so a player/club is one Kleos row across seasons.
- Never commit scraped bulk datasets into git.

## Allowed sources (current)

| Source | Use for | Redistribution | Notes |
|--------|---------|----------------|-------|
| **FBref** (via [`soccerdata`](https://soccerdata.readthedocs.io/) or equivalent) | Clubs, players, PlayerSeason counting/expected stats | Do **not** republish raw FBref dumps in this repo | Primary ingest path (`scripts/ingest_fbref_pl_laliga.py`). Respect crawl delays; personal/research use only unless you obtain broader rights. |
| **TheSportsDB** | Club crest **HTTPS badge URLs** | Hotlink badge URLs; do not mirror binaries into git | Primary club crest path in `scripts/enrich_identity_media.py` (`strBadge`). Research/UI display; respect TheSportsDB terms. |
| **Wikimedia** (Wikidata + English Wikipedia + Commons APIs) | Player photos; club crest fallback; player **height** (Wikidata P2048) | Hotlink image URLs only; **do not** mirror binaries into git | **Players photos:** free licenses only (CC0 / PD / CC-BY / CC-BY-SA / GFDL). Height via Wikidata quantity claims. Never scrape Google Images or Transfermarkt/FBref CDNs. |
| **Wikipedia / Wikidata** | Player height (P2048); preferred foot when stated in article/infobox | Derived fields only | Bio enricher: `scripts/enrich_player_bio.py`. |
| **FBref player profile pages** | Preferred foot (+ height fallback) | Do **not** republish raw HTML dumps | Season tables omit bio; profiles expose `Footed:` / `cm`. Optional path in `enrich_player_bio.py` (crawl delay; may 403 without a residential IP). |
| **Manual / first-party curated CSVs** | Small identity patches | OK if you created them | Use `scripts/import-identities.py`. |
| **StatsBomb Open Data** | Event research, not full PL/La Liga season coverage | Allowed under StatsBomb open-data terms | Not the PL/La Liga completeness path. |

## Not allowed without explicit license

- Redistributing Transfermarkt scrapes, Opta/StatsPerform dumps, or paid API extracts in this repository.
- Hotlinking Transfermarkt / FBref CDN player photos or club crests.
- Shipping someone else’s full historical database as “sample data”.
- Ignoring robots/rate limits or ToS of a scraped site.

## Identity media checklist

Before enriching photos/crests against a shared database:

- [ ] Backend migration applied (`photo_*` / `crest_*` columns).
- [ ] Prefer **clubs first**, then players (`python3 scripts/enrich_identity_media.py clubs`).
- [ ] Start with `--dry-run` and a small `--limit`.
- [ ] Players: prefer free licenses; leave null when none exists (UI uses initials).
- [ ] Clubs: prefer Wikidata/Wikipedia crest URLs; do not commit downloaded logo files.
- [ ] Keep Wikimedia User-Agent + delay etiquette in the enricher.

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
- Birth dates: FBref season tables usually expose **birth year only**. Ingest stores `YYYY-07-01` as a mid-year age anchor with `dateOfBirthPrecision=YEAR` so the UI shows the year alone (age still uses 1 July).

## Ingest checklist

Before running a full load against a shared database:

- [ ] Backend migrations applied (including player/club `fbref_id` uniqueness).
- [ ] You have read FBref / Sports Reference terms for your use case.
- [ ] Start with `--dry-run` and a single season (`--seasons 2024/25`).
- [ ] Confirm duplicate skips on a second run (idempotent).
- [ ] Do not commit `data/cache/` or downloaded CSVs.

### Runtime notes

- Chrome runs **headless by default** (`--headless`; use `--no-headless` only if you need a visible window for captcha debugging). Cached FBref HTML under `~/soccerdata/data/FBref` avoids re-downloads.
- Each league-season is upserted before the next fetch, so a long run can be resumed after interruption.
- soccerdata season ids use `YYZZ` (e.g. `2122` for 2021/22). Do **not** pass a bare year like `2021` — soccerdata treats that as 2020/21.
- Common non-FIFA FBref nationality aliases (e.g. `KVX` → `KOS`, `MTQ`/`GLP` → `FRA`) are mapped in the ingest script before API calls.

## Demo seed

There is **no** fake demo-seed pipeline. Local UX should use the real ingest (possibly one season) or small hand-authored CSVs you own.
