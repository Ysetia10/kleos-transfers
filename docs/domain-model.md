# Kleos Transfers Domain Model

This document is the single source of truth for the Kleos Transfers domain model. It records agreed concepts, their responsibilities, and the questions that must be resolved before implementation.

## Domain Modeling Principles

Kleos Transfers separates its domain into three categories:

1. **Identity Entities** — permanent information that rarely changes.
2. **Historical Entities** — information that changes every season.
3. **Prediction Entities** — Kleos-specific entities used for prediction and evaluation.

The model follows these principles:

- Identity and historical data must never be mixed.
- Everything that changes over time belongs in a historical entity.
- Every entity must exist because it helps answer a business question.
- Avoid duplicate data whenever possible.
- Relationships should be explicit and meaningful.
- Keep the model extensible for future prediction versions.

## Player

### Purpose

Represent the permanent identity of a football player. Player identity never stores club membership, seasonal stats, market value, contracts, injuries, or transfers.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `fullName` | String (2–100) | Display name |
| `dateOfBirth` | LocalDate | Must be past or present |
| `nationality` | String (3) | FIFA association code (`ENG`, `GER`, `NED`, …) |
| `heightCm` | Integer | Range 140–230 |
| `preferredFoot` | Enum | `LEFT`, `RIGHT`, `BOTH` |
| `primaryPosition` | Enum | Pitch position code (`GK` … `ST`) |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |

### Relationships

None yet. Future historical entities (`PlayerSeason`, `Transfer`, `Contract`, `Injury`) will reference Player.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/players`.
- Nationality intentionally uses football codes, not ISO country codes.

## Club

### Purpose

_To be finalized._ Permanent club identity (name, country/association, founding metadata). Seasonal league membership and squad context belong in `ClubSeason`.

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

Next identity-layer target after Player.

## Manager

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Season

### Purpose

_To be finalized._ Calendar/competition season identity (for example `2025/26`).

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Tournament

### Purpose

_To be finalized._ Competition identity (Premier League, Champions League, …).

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## PlayerSeason

### Purpose

_To be finalized._ Season-scoped player performance and context.

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## ClubSeason

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Transfer

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Prediction

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Contract

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Injury

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## National Team

### Purpose

_To be finalized._

### Attributes

_To be finalized._

### Relationships

_To be finalized._

### Notes

_To be finalized._

## Open Questions

- Soft-delete vs hard-delete for identity entities once historical foreign keys exist.
- Natural-key uniqueness rules for Player (name + DOB + nationality is imperfect).
- Club naming across multi-team cities and legal entity vs sporting brand.
- Season boundary model (calendar year vs competition year).
- Package-by-feature migration timing before Club/Manager land.
