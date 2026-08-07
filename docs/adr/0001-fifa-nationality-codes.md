# ADR 0001: FIFA association nationality codes

- **Status:** Accepted
- **Date:** 2026-08-05

## Context

Player, manager, and club country fields need a stable three-letter vocabulary shared by football data sources. ISO 3166-1 alpha-3 (`GBR`, `DEU`, `NLD`) does not match how football datasets label associations (`ENG`, `GER`, `NED`).

## Decision

Use **FIFA association codes** validated by `FootballNationalityCodes` / `@FootballNationalityCode`. Ingest may map non-FIFA FBref aliases (e.g. `KVX` → `KOS`) before API calls; the API itself does not relax validation.

## Consequences

- Positive: Aligns with FBref-style football data and keeps identity keys stable.
- Negative: Occasional mapping work when sources emit ISO or territory codes.
- Follow-ups: Keep ingest alias table documented in `docs/data-sourcing.md`.
