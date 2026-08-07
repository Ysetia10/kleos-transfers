# ADR 0002: Identity soft-delete lifecycle

- **Status:** Accepted
- **Date:** 2026-08-05

## Context

Identity rows (Player, Club, Manager, …) are referenced by historical and prediction foreign keys. Hard-deleting an identity would either cascade-destroy history or leave orphaned FKs.

## Decision

Identity entities extend `IdentityEntity` with `deletedAt`. `DELETE` APIs set `deletedAt` and free uniqueness slots (e.g. suffix `#<id>` on normalized keys / `fbrefId`). Reads filter soft-deleted rows via Hibernate `@SQLRestriction`.

## Consequences

- Positive: Historical FKs remain valid; identities can be recreated after soft delete.
- Negative: Tables retain tombstones; queries must consistently filter `deleted_at`.
- Follow-ups: None for v1 — do not switch to hard delete without a migration plan.
