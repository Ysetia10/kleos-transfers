# ADR 0005: Real-data ingest; no fake demo seed

- **Status:** Accepted
- **Date:** 2026-08-06

## Context

Local UX and prediction validation need historical PL/La Liga data. Shipping a fabricated “demo seed” would teach the wrong product story and risk redistributing third-party data.

## Decision

- Load history via **`scripts/ingest_fbref_pl_laliga.py`** (and small first-party CSVs you own).
- **Do not** commit scraped dumps or a fake demo-seed pipeline (issue #31 closed as not planned).
- Follow `docs/data-sourcing.md` for attribution and rate limits.

## Consequences

- Positive: Local data matches real seasons; validation is meaningful.
- Negative: First setup requires Postgres + ingest time; FBref Expected stats may be unavailable (#37).
- Follow-ups: Recover xG/xA when a ToS-compatible source exists.
