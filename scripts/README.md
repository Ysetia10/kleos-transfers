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

## `enrich_identity_media.py`

Resolves club crest URLs (TheSportsDB badges, Wikimedia fallback) and free-licensed Wikimedia player photos, then `PUT`s URL + attribution/license/source onto identity media endpoints. Hotlinks only — does not download binaries into git or scrape Google Images.

```bash
# backend must be running (migration V18+)
python3 scripts/enrich_identity_media.py clubs --dry-run --limit 10
python3 scripts/enrich_identity_media.py clubs --include-existing
python3 scripts/enrich_identity_media.py players --workers 8
python3 scripts/enrich_identity_media.py players --limit 100
```

Prefer clubs first. Missing media stays null; the UI falls back to initials. Policy: [`docs/data-sourcing.md`](../docs/data-sourcing.md).

## `enrich_player_bio.py`

Fills missing `heightCm` / `preferredFoot` on player identities:

- **Height:** Wikidata `P2048` (name search + nationality filter)
- **Preferred foot:** Wikipedia article/infobox (`left-footed` / `right-footed`), then optional FBref profile (`Footed:`)

```bash
python3 scripts/enrich_player_bio.py --dry-run --limit 20
# full backfill: SPARQL height batches + concurrent Wikipedia foot/height fallback
python3 scripts/enrich_player_bio.py --skip-fbref --workers 8
# heights only (fast)
python3 scripts/enrich_player_bio.py --skip-fbref --skip-foot --workers 8
# upgrade YEAR → full day DOB from Wikidata (year must match FBref year)
python3 scripts/enrich_player_bio.py --dob-only --workers 8
python3 scripts/enrich_player_bio.py --limit 200                # also try FBref (~3s/player; may 403)
```

Season-stat ingest intentionally leaves these null; run this enricher after identities exist.

## `ingest_fbref_pl_laliga.py`

**Primary historical load** for the top five European leagues (PL, La Liga, Bundesliga, Serie A, Ligue 1), seasons **2016/17–2025/26**.

Reads FBref via `soccerdata`, then upserts tournaments, seasons, clubs, players, club-seasons, and player-seasons through the Kleos bulk APIs. Player/club matching uses `fbrefId` so re-runs are idempotent.

```bash
pip install -r scripts/requirements-ingest.txt
# backend must be running
./scripts/ingest_fbref_pl_laliga.py --dry-run --seasons 2024/25
./scripts/ingest_fbref_pl_laliga.py --seasons 2024/25
# new leagues only (skip already-loaded PL + La Liga)
./scripts/ingest_fbref_pl_laliga.py --leagues "GER-Bundesliga,ITA-Serie A,FRA-Ligue 1"
./scripts/ingest_fbref_pl_laliga.py   # full 2016/17 … 2025/26 window, all five
# after ingest: crests + bio + DOB + photos
./scripts/run_top5_post_ingest_enrich.sh
```

Policy and attribution: [`docs/data-sourcing.md`](../docs/data-sourcing.md).

Do not commit downloaded caches or bulk CSVs (`data/` is gitignored).

## `ingest_transfers_from_wikipedia.py`

Loads **dated** summer/winter window transfers from Wikipedia list pages (MediaWiki API) into `Transfer` rows for a predict-to season (default `2026/27`). Prefer this over PlayerSeason-inferred July-1 rows for the live Transfers tab.

```bash
# backend running; 2026/27 season shell present
./scripts/ingest_transfers_from_wikipedia.py --dry-run --pages en-summer-2026
./scripts/ingest_transfers_from_wikipedia.py --season 2026/27 --pages en-summer-2026 --sleep 1.2
# when rate limits allow:
./scripts/ingest_transfers_from_wikipedia.py --pages top5-summer-2026 --sleep 1.5
```

Policy: Wikipedia is allowed; Transfermarkt scrapes are not. See [`docs/data-sourcing.md`](../docs/data-sourcing.md).

## `ensure_predict_seasons.py`

Creates **forward Season identity shells** (default `2026/27`) so the simulator can project upcoming campaigns. Does **not** scrape FBref outcomes for incomplete seasons.

```bash
# backend must be running
./scripts/ensure_predict_seasons.py
./scripts/ensure_predict_seasons.py --dry-run
```

## `validate_predictions_season.py`

Backtests the active heuristic engine against a **completed** season: creates predictions via the API, evaluates them into `PredictionEvaluation`, and writes MAE/RMSE summaries under `research/validation/`.

```bash
# backend running; historical seasons loaded
./scripts/validate_predictions_season.py --season 2024/25 --dry-run
./scripts/validate_predictions_season.py --season 2024/25 --limit 200
```

Methodology: [`docs/prediction-validation.md`](../docs/prediction-validation.md).

Requires `psql` for candidate discovery against the local Postgres DB (default `postgresql://kleos:kleos@localhost:5432/kleos_transfers`).
