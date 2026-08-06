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
| `deletedAt` | Instant (nullable) | Soft-delete marker; null means active |

### Relationships

None yet. Future historical entities (`PlayerSeason`, `Transfer`, `Contract`, `Injury`) will reference Player.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/players`.
- Nationality intentionally uses football codes, not ISO country codes.
- Identity entities use soft delete: `DELETE` sets `deletedAt`; reads exclude deleted rows. Historical FKs can still resolve the underlying id if needed later.

## Club

### Purpose

Represent the permanent identity of a football club. Club identity never stores league membership, manager, squad, stadium season details, market value, or transfers.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `name` | String (2–120) | Official/common club name |
| `shortName` | String (2–40) | Compact display name |
| `countryCode` | String (3) | FIFA association code (`ESP`, `ENG`, …) |
| `foundedYear` | Integer (optional) | Range 1800–2100 when present |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

### Relationships

None yet. Future historical entities (`ClubSeason`, `Transfer`, `Contract`) will reference Club.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/clubs`.
- Active uniqueness: `(nameNormalized, countryCode)` (`nameNormalized` is lowercase `name`, not exposed on the API). Soft delete appends `#<id>` to `nameNormalized` so the same club name can be recreated.
- Seasonal context belongs in `ClubSeason`, not on Club.

## Manager

### Purpose

Represent the permanent identity of a football manager as a person. Manager identity never stores the club they manage, tactical philosophy, formation, or results — those change per appointment and belong to `ManagerSeason`.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `fullName` | String (2–100) | Display name |
| `dateOfBirth` | LocalDate | Must be past or present |
| `nationality` | String (3) | FIFA association code |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

### Relationships

None yet. `ManagerSeason` will link Manager to Club and Season.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/managers`.
- Deliberately mirrors Player: a manager is a person, not a role.

## Season

### Purpose

Represent a football competition season as a permanent identity: a named date range that historical entities reference. A Season never stores fixtures, standings, tournament membership, or club/player stats.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `label` | String (4–20) | Display/key label: European `YYYY/YY` (`2024/25`) or calendar `YYYY` (`2024`) |
| `startDate` | LocalDate | Inclusive season start |
| `endDate` | LocalDate | Inclusive season end; must be strictly after `startDate` |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `labelNormalized` (lowercase `label`) enforces case-insensitive uniqueness among active seasons. Soft delete appends `#<id>` so the label can be reused.

### Relationships

None yet. `PlayerSeason`, `ClubSeason`, `ManagerSeason`, `Transfer`, and related historical entities will reference Season by id.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/seasons` (+ `/bulk`).
- **Temporal boundary rules:** a season is the closed range `[startDate, endDate]`. Historical facts attach to a Season id; they do not store a separate year field. Overlapping date ranges across different labels are allowed (e.g. European `2024/25` and calendar `2024`) because competitions use different calendars. Uniqueness is on label only.
- Label validation: `YYYY/YY` requires the second year to be the calendar successor of the first (`2024/25` valid, `2024/26` invalid).

## Tournament

### Purpose

Represent a football competition as a permanent identity (Premier League, Champions League, World Cup, …). Tournament identity never stores season editions, standings, fixtures, or participating clubs — those change over time and belong to historical entities.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `name` | String (2–120) | Official/common competition name |
| `shortName` | String (2–40) | Compact display name (`EPL`, `UCL`) |
| `confederation` | Enum | `UEFA`, `CONMEBOL`, `CONCACAF`, `CAF`, `AFC`, `OFC`, `FIFA` |
| `type` | Enum | `LEAGUE`, `CUP`, `SUPER_CUP` |
| `countryCode` | String (3, optional) | FIFA association code for domestic competitions; null for continental/worldwide |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `nameNormalized` (lowercase `name`) enforces case-insensitive uniqueness among active tournaments. Soft delete appends `#<id>` so the name can be reused.

### Relationships

None yet. Historical competition rows (e.g. club participation in a season) will reference Tournament by id.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/tournaments` (+ `/bulk`).
- Domestic example: Premier League → `UEFA` / `LEAGUE` / `ENG`.
- Continental example: UEFA Champions League → `UEFA` / `CUP` / no country.

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

## Decisions

- **Identity soft delete** — identity entities set `deletedAt` instead of hard-deleting rows, so historical foreign keys remain valid after an identity is removed from active APIs.
- **Club uniqueness** — clubs are unique on case-insensitive `name` + `countryCode` via `nameNormalized`; soft delete suffixes `nameNormalized` so re-creation is allowed.

## Open Questions

- Natural-key uniqueness rules for Player (name + DOB + nationality is imperfect).
- Whether Club needs a separate legal/sporting brand distinction later.
- Season boundary model (calendar year vs competition year).
