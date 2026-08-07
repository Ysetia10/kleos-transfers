# ADR 0004: Prediction model versioning on runs

- **Status:** Accepted
- **Date:** 2026-08-06

## Context

The heuristic engine will evolve (v0 → v0.1 → v0.2 → v1…). Rewriting past prediction rows would erase the audit trail needed for completed-season validation.

## Decision

Persist `modelVersion` on `PredictionRun` (e.g. `v0.2-heuristic`). The active `PredictionEngine` bean returns the current version string. Past runs keep the version that produced them. Validation artifacts under `research/validation/` are keyed by model version.

## Consequences

- Positive: Comparable baselines; safe algorithm swaps via `PredictionEngine`.
- Negative: Multiple versions coexist in the DB; dashboards must not mix them naively.
- Follow-ups: v1 must beat the latest published baseline (`docs/prediction-validation.md`).
