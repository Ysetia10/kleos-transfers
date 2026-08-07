# ADR 0003: Public API versioning under `/api/v1`

- **Status:** Accepted
- **Date:** 2026-08-05

## Context

Frontend clients, ingest scripts, and future consumers need a stable HTTP contract. Breaking changes without a version prefix would force silent client breakage.

## Decision

Expose the public HTTP API under **`/api/v1`**. OpenAPI/Swagger documents that surface (`/v3/api-docs`, `/swagger-ui.html`). Additive changes stay in v1; breaking changes require `/api/v2` (or equivalent) rather than silent renames.

## Consequences

- Positive: Clear compatibility boundary for scripts and UI.
- Negative: Duplicate paths during a future major migration.
- Follow-ups: Keep SpringDoc annotations current as modules grow.
