# Data sourcing and licensing policy

Kleos Transfers is an open-source research/product project. Historical football data is loaded only from sources we can attribute and whose terms we can respect. This document is the checklist before any ingest work ships.

## Goals

- Load the **top five European leagues** (Premier League, La Liga, Bundesliga, Serie A, Ligue 1) identity + player-season history for seasons **2016/17 through 2025/26** (inclusive).
- **Predict-to seasons** (e.g. **2026/27**) are allowed as Season identity shells so the simulator can project before outcomes exist. Do **not** scrape incomplete FBref PlayerSeason tables for those labels — create the shell with `scripts/ensure_predict_seasons.py` and keep validation on completed seasons only.
- Prefer **stable external IDs** (`fbrefId`) so a player/club is one Kleos row across seasons.
- Never commit scraped bulk datasets into git.

## Allowed sources (current)

| Source | Use for | Redistribution | Notes |
|--------|---------|----------------|-------|
| **FBref** (via [`soccerdata`](https://soccerdata.readthedocs.io/) or equivalent) | Clubs, players, PlayerSeason counting/expected stats | Do **not** republish raw FBref dumps in this repo | Primary ingest path (`scripts/ingest_fbref_pl_laliga.py`). Respect crawl delays; personal/research use only unless you obtain broader rights. |
| **TheSportsDB** | Club crest **HTTPS badge URLs** | Hotlink badge URLs; do not mirror binaries into git | Primary club crest path in `scripts/enrich_identity_media.py` (`strBadge`). Research/UI display; respect TheSportsDB terms. |
| **Wikimedia** (Wikidata + English Wikipedia + Commons APIs) | Player photos; club crest fallback; player **height** (Wikidata P2048) | Hotlink image URLs only; **do not** mirror binaries into git | **Players photos:** free licenses only (CC0 / PD / CC-BY / CC-BY-SA / GFDL). Height via Wikidata quantity claims. Never scrape Google Images or Transfermarkt/FBref CDNs. |
| **Wikipedia / Wikidata** | Player height (P2048); preferred foot (P8006 batch + optional Wikipedia infobox) | Derived fields only | Bio enricher: `scripts/enrich_player_bio.py` (`--foot-only` uses Wikidata SPARQL). |
| **FBref player profile pages** | Preferred foot (+ height fallback) | Do **not** republish raw HTML dumps | Season tables omit bio; profiles expose `Footed:` / `cm`. Optional path in `enrich_player_bio.py` (crawl delay; may 403 without a residential IP). |
| **Manual / first-party curated CSVs** | Small identity patches; career leaderboards | OK if you created them | Identities: `scripts/import-identities.py`. Career leaders: `research/career-leaders/` + `scripts/import_career_leaders.py` (Wikipedia-attributed top-N). |
| **Understat** (via [`soccerdata`](https://soccerdata.readthedocs.io/)) | Player-season **xG / xA** backfill when FBref Expected cols are absent | Do **not** republish raw Understat dumps | Secondary enricher: `scripts/enrich_xg_from_understat.py`. Research/UI aggregates only; respect Understat / soccerdata terms. |
| **Kleos PlayerSeason diffs** | Completed transfers inferred from consecutive club changes | First-party derived | `scripts/infer_transfers_from_seasons.py` → `Transfer` rows with `status=COMPLETED` (season-start dates). Good for history; weak for live windows. |
| **English Wikipedia transfer lists** | Latest summer/winter window deals with real dates + fees (top leagues) | Derived Transfer rows only; credit Wikipedia | `scripts/ingest_transfers_from_wikipedia.py` via MediaWiki API (`source=wikipedia:…`). Prefer this for the **upcoming** season board. Do **not** scrape Transfermarkt. |
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
- [ ] Measure coverage: `python3 scripts/enrich_identity_media.py players --stats`.
- [ ] Start with `--dry-run` and a small `--limit`.
- [ ] Players: nationality + birth-year Wikipedia disambiguation; free licenses only; leave null when none exists (UI uses initials).
- [ ] Clubs: prefer TheSportsDB badge URLs (Wikimedia fallback opt-in); do not commit downloaded logo files.
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

## Expected goals (xG / xA)

FBref season tables often **omit** Expected columns in the HTML soccerdata scrapes (see #37). When that happens:

1. Keep counting stats from FBref ingest.
2. Backfill `player_seasons.xg` / `xa` with `scripts/enrich_xg_from_understat.py`.
3. Re-run `scripts/validate_predictions_season.py` after a material backfill and refresh `research/validation/` summaries.

Do not invent a shots→xG proxy without labelling it as non-xG.

## Pitch roles (precise positions)

FBref **season** squad tables usually only expose `GK` / `DF` / `MF` / `FW`, which ingest maps to coarse `GK` / `CB` / `CM` / `ST`. Pitch lineup placement and exact-role squad competition need lateral roles (`RB` / `LB` / `RW` / `LW` / …).

**Preferred ENG path (fast):** Premier League PulseLive season staff (`footballapi.pulselive.com`) publishes detailed `positionInfo` per club-season in one request per team — no per-match crawl.

```bash
python3 scripts/enrich_positions_from_premierleague.py --seasons 2024/25 2025/26 --dry-run
python3 scripts/enrich_positions_from_premierleague.py --seasons 2024/25 2025/26
```

The script maps PulseLive long club names (`West Ham United`, `Nottingham Forest`, …) onto Kleos short names via `CLUB_ALIAS_GROUPS`. Without those aliases, entire clubs silently skip enrichment. Coarse PulseLive labels (`Defender`, `Winger`, multi-side) are left unchanged.

**Cross-league path (accurate, slow):** FBref **match** player summary tables via `soccerdata`. Cold scrapes use headless Chrome and Sports-Reference’s ~10 req/min cap — often **3–5 minutes per match** on first download. Cached HTML under `~/soccerdata/data/FBref` makes re-runs fast. Use for ESP/GER/ITA/FRA (ENG prefers PulseLive above).

```bash
# Stratified sample (default): cover every club with fewer matches; apply in batches; reuse cache first.
python3 scripts/enrich_positions_from_fbref_lineups.py --seasons 2024/25 --leagues ESP-La\ Liga --max-matches 40
# Resume / extend after a partial run (already-cached matches are near-instant):
python3 scripts/enrich_positions_from_fbref_lineups.py --seasons 2024/25 --leagues ESP-La\ Liga --max-matches 80 --batch-size 5
```

There is no safe way to “speed past” FBref’s rate limit without a licensed feed; the real multi-league speedup is a PulseLive-style staff API or paid positions dump per league.

Understat season `position` strings (`D M`, `F M S`) are too coarse for left/right placement — do not use them as the primary role source. Keep coarse fallbacks only when match minutes are unavailable; the UI shows a “role precision unavailable” state in that case.

**Not used:** Transfermarkt scrapes (disallowed). Paid bulk APIs (foot.io / API-Football) are optional later if we need multi-league speed with a licensed key.

## Transfers

- **COMPLETED** — preferred path: `scripts/infer_transfers_from_seasons.py` (derived from consecutive PlayerSeason club changes already in Kleos). Fee usually null.
- **ANNOUNCED** / **RUMOURED** — import via `POST /api/v1/transfers` (or bulk) with explicit `status`. Never use rumours as evaluation ground truth.
- Transfermarkt scrapes remain **disallowed** for redistribution in this repo.

## Demo seed

There is **no** fake demo-seed pipeline. Local UX should use the real ingest (possibly one season) or small hand-authored CSVs you own.
