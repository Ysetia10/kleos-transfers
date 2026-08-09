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
| `dateOfBirth` | LocalDate | Must be past or present. When only birth year is known, stored as `YYYY-07-01` mid-year age anchor |
| `dateOfBirthPrecision` | `DAY` \| `YEAR` | `YEAR` = UI shows year only; age still uses stored date. Defaults to `DAY` on manual create |
| `nationality` | String (3) | FIFA association code (`ENG`, `GER`, `NED`, …) |
| `heightCm` | Integer (optional) | Range 140–230 when present; often omitted by season-stat sources |
| `preferredFoot` | Enum (optional) | `LEFT`, `RIGHT`, `BOTH` when present |
| `primaryPosition` | Enum | Pitch position code (`GK` … `ST`) |
| `fbrefId` | String (optional) | Stable FBref-derived ingest key; unique when set |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker; null means active |

Internal: `fullNameNormalized` (lowercase `fullName`) plus `dateOfBirth` + `nationality` enforce active uniqueness. Soft delete appends `#<id>` to `fullNameNormalized` (and to `fbrefId` when set).

### Relationships

Referenced by `PlayerSeason`, `Transfer`, `Contract`, and `Injury`.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/players`.
- Nationality intentionally uses football codes, not ISO country codes.
- Identity entities use soft delete: `DELETE` sets `deletedAt`; reads exclude deleted rows. Historical FKs can still resolve the underlying id if needed later.
- **One player row:** create/update reject conflicts on the natural key and on `fbrefId`. Bulk import skips existing keys. Prefer `fbrefId` during ingest so name spelling drift does not create duplicates.

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
| `fbrefId` | String (optional) | Stable FBref-derived ingest key; unique when set |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

### Relationships

Referenced by `ClubSeason`, `PlayerSeason`, `ManagerSeason`, `Transfer`, and `Contract`.

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/clubs`.
- Active uniqueness: `(nameNormalized, countryCode)` (`nameNormalized` is lowercase `name`, not exposed on the API). Soft delete appends `#<id>` to `nameNormalized` (and `fbrefId` when set) so the same club can be recreated.
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

Referenced by `PlayerSeason`, `ClubSeason`, `ManagerSeason`, and `Transfer`. `Contract` and `Injury` are date-ranged rather than season-scoped, so they do not reference Season.

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

Represent a player's performance and role for one club in one season. This is the core historical input for adaptation predictions. Permanent identity attributes stay on Player; mid-season transfers produce a second PlayerSeason row for the destination club.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `player` | FK → Player | Required |
| `club` | FK → Club | Required |
| `season` | FK → Season | Required |
| `appearances` | Integer ≥ 0 | Matches played |
| `minutesPlayed` | Integer ≥ 0 | Total minutes; must be ≥ appearances when appearances > 0 |
| `goals` | Integer ≥ 0 | Goals scored |
| `assists` | Integer ≥ 0 | Assists |
| `xg` | Decimal ≥ 0 | Expected goals |
| `xa` | Decimal ≥ 0 | Expected assists |
| `primaryPosition` | Position enum | Main position in this spell (may differ from Player identity) |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `uniquenessKey` is `{playerId}:{clubId}:{seasonId}` while active; soft delete appends `#{id}`.

### Relationships

- Many PlayerSeasons → one Player
- Many PlayerSeasons → one Club
- Many PlayerSeasons → one Season

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/player-seasons` (+ `/bulk`).
- **Unique per (player, club, season).** Same player at two clubs in one season is allowed.
- Stats chosen for prediction usefulness (workload, output, finishing/creation quality, role). Cards, ratings, and detailed position splits deferred.

## ClubSeason

### Purpose

Represent a club competing in one season under its primary tournament (usually the domestic league). This is the historical spine for club context used by adaptation predictions. It does not store match results, squad lists, or cup runs.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `club` | FK → Club | Required |
| `season` | FK → Season | Required |
| `tournament` | FK → Tournament | Primary competition for that club-season |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `uniquenessKey` is `{clubId}:{seasonId}` while active; soft delete appends `#{id}` so the pair can be recreated.

### Relationships

- Many ClubSeasons → one Club
- Many ClubSeasons → one Season
- Many ClubSeasons → one Tournament (primary league/competition)
- Future: `ManagerSeason` / `PlayerSeason` may reference ClubSeason or the same Club+Season pair

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/club-seasons` (+ `/bulk`).
- **One active row per (club, season).** Cup/continental participation is intentionally out of scope for v1 — add a separate participation entity later if needed.
- No finishing position, points, or xG yet — add only when a prediction feature needs them.

## ManagerSeason

### Purpose

Represent a manager's appointment at a club for a season. This is historical context for managerial approach in transfer predictions. Tactical philosophy/style fields are deferred until a prediction feature needs them.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `manager` | FK → Manager | Required |
| `club` | FK → Club | Required |
| `season` | FK → Season | Required |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `uniquenessKey` is `{managerId}:{clubId}:{seasonId}` while active; soft delete appends `#{id}`.

### Relationships

- Many ManagerSeasons → one Manager
- Many ManagerSeasons → one Club
- Many ManagerSeasons → one Season

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/manager-seasons` (+ `/bulk`).
- **Uniqueness is per (manager, club, season).** Different managers at the same club in the same season are allowed (mid-season changes / caretakers). Appointment start/end dates can be added later if needed.
- No philosophy, formation, or press-intensity fields yet.

## Transfer

### Purpose

Record a player moving between clubs (or into/out of free agency). Transfers are historical facts used later by prediction what-if scenarios. Creating a transfer does not generate a prediction.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `player` | FK → Player | Required |
| `fromClub` | FK → Club (optional) | Null = signing from free agency / unknown prior club |
| `toClub` | FK → Club (optional) | Null = release to free agency |
| `season` | FK → Season | Season context for the move |
| `transferDate` | LocalDate | Effective date of the move |
| `feeEur` | Decimal (optional) | Transfer fee in EUR; null = undisclosed / not applicable |
| `type` | Enum | `PERMANENT`, `LOAN`, `FREE`, `LOAN_RETURN` |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `uniquenessKey` is `{playerId}:{date}:{from|none}:{to|none}:{type}` while active; soft delete appends `#{id}`.

### Relationships

- Many Transfers → one Player
- Many Transfers → zero-or-one from Club / to Club
- Many Transfers → one Season

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/transfers` (+ `/bulk`).
- At least one of `fromClub` / `toClub` is required; they must differ when both are set.
- Unique per player + date + from + to + type so the same event is not imported twice.

## Contract

### Purpose

Record a player's contract window at a club. Contracts supply expiry pressure and renewal context: a player entering the final year of a deal moves under different conditions than one recently extended.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `player` | FK → Player | Required |
| `club` | FK → Club | Required |
| `startDate` | LocalDate | First day of the contract |
| `endDate` | LocalDate | Expiry date; must be after `startDate` |
| `releaseClauseEur` | Decimal (optional) | Null = none or undisclosed |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `uniquenessKey` is `{playerId}:{clubId}:{startDate}` while active; soft delete appends `#{id}`.

### Relationships

- Many Contracts → one Player
- Many Contracts → one Club

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/contracts` (+ `/bulk`).
- **Relationship to Transfer:** Contract and Transfer are independent records. A transfer usually implies a new contract at the destination club, but Kleos does not create one automatically — the two are sourced separately and a transfer can be known before its contract terms are. Join them through `playerId` + `clubId` when both are needed.
- **Renewals are new rows,** not edits: uniqueness is per start date, so a player can hold several historical contracts at the same club. This preserves the pre-renewal state that a prediction made at the time would have used.
- No wage field yet. Wage data needs its own sourcing decision and no current prediction consumes it.

## Injury

### Purpose

Record a single injury spell for a player. Injury history is availability and adaptation context: recent time out is a strong negative signal for minutes predictions after a transfer.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `player` | FK → Player | Required |
| `injuryType` | String (≤80) | Free text, e.g. `Hamstring strain`, `ACL rupture` |
| `severity` | Enum | `MINOR`, `MODERATE`, `SEVERE` |
| `startDate` | LocalDate | First day unavailable |
| `endDate` | LocalDate (optional) | Return date; null = still out. May equal `startDate` |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

Internal: `injuryTypeNormalized` (lowercase, trimmed `injuryType`) feeds the `uniquenessKey` `{playerId}:{startDate}:{injuryTypeNormalized}`; soft delete appends `#{id}`.

Derived on the API: `daysOut` (inclusive day count, null while ongoing) and `ongoing`.

### Relationships

- Many Injuries → one Player

### Notes

- Implemented in backend Version 0.2.
- API: `/api/v1/injuries` (+ `/bulk`).
- **`daysOut` is derived, not stored,** so it cannot drift from the date range.
- **`injuryType` is free text, `severity` is an enum.** Injury names are open-ended and vary by source; severity is the part predictions actually bucket on.
- An injury is not tied to a Season. Spells cross season boundaries, so consumers filter by date range instead.
- No club, matches-missed, or recurrence-link fields yet. Matches missed needs fixture data that Kleos does not model.

## PredictionRun

### Purpose

Audit wrapper for one execution of the prediction engine. Stores the model version so later algorithm changes do not rewrite historical outputs.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `modelVersion` | String | e.g. `v0.2-heuristic` |
| `note` | String (optional) | Free-text scenario note |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

### Relationships

- One PredictionRun → many Predictions

### Notes

- Implemented in backend Version 0.3.
- Created automatically when `POST /api/v1/predictions` runs a scenario. Read via `/api/v1/prediction-runs/{id}`.

## Prediction

### Purpose

One transfer what-if: predicted first-season performance of a player at a target club. This is the product object — metrics, scores, and explanations travel together.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `run` | FK → PredictionRun | Required |
| `player` | FK → Player | Required |
| `targetClub` | FK → Club | Required |
| `season` | FK → Season | Season being predicted |
| `predictedMinutes` | Integer | Expected minutes |
| `predictedGoals` / `predictedAssists` | Decimal | Counting-stat projections |
| `predictedXg` / `predictedXa` | Decimal | Expected-stat projections |
| `predictedMarketValueEur` | Decimal (optional) | Coarse end-of-season value |
| `compatibilityScore` | Decimal 0–100 | Transfer fit / adaptation score |
| `confidenceScore` | Decimal 0–100 | Input-coverage confidence |
| `createdAt` / `updatedAt` | Instant | Audited timestamps |
| `deletedAt` | Instant (nullable) | Soft-delete marker |

### Relationships

- Many Predictions → one PredictionRun / Player / Club / Season
- One Prediction → many PredictionExplanations
- One Prediction → zero-or-one PredictionEvaluation

### Notes

- Implemented in backend Version 0.3.
- API: `POST|GET /api/v1/predictions`, `GET /api/v1/predictions/{id}`, `DELETE /api/v1/predictions/{id}`.
- **Product decision:** metrics live on one row (not separate tables per metric). Users predict a scenario, not "minutes alone".
- **Current engine (`v0.2-heuristic`)** is deterministic and explainable: weighted recent minutes + age + injury + softened squad competition, with a separate GK starter/backup pathway; goals/assists/xG/xA from historical per-90 × predicted minutes; market value from age band × contribution; compatibility/confidence from factor rules. Replaceable via `PredictionEngine` without changing the API shape.
- Context is loaded **as of** the target season start (history and squad exclude the target season) so completed-season evaluation does not leak outcomes. See [`prediction-validation.md`](prediction-validation.md).
- The same player/club/season may be predicted multiple times (different runs / model versions).

## PredictionExplanation

### Purpose

One human-readable factor behind a prediction — the product's explainability surface for the UI.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `prediction` | FK → Prediction | Required |
| `factorCode` | String | Stable code (`AGE_PROFILE`, `INJURY_BURDEN`, …) |
| `label` | String | Short UI label |
| `direction` | Enum | `POSITIVE`, `NEGATIVE`, `NEUTRAL` |
| `impact` | Decimal | Magnitude on a 0–100 style scale |
| `detail` | String | Human-readable sentence |
| `sortOrder` | Integer | Display order |

### Notes

- Owned by the parent prediction (cascade). No independent soft-delete lifecycle.
- Factor codes are stable for UI grouping even if wording changes.

## PredictionEvaluation

### Purpose

Post-season comparison of a prediction against the observed `PlayerSeason` for the same player/club/season.

### Attributes

| Attribute | Type | Notes |
|-----------|------|-------|
| `id` | UUID | Surrogate primary key |
| `prediction` | FK → Prediction | Unique |
| `actualMinutes` / `actualGoals` / `actualAssists` | Integer | Observed outcomes |
| `actualXg` / `actualXa` | Decimal | Observed expected stats |
| `minutesError` / `goalsError` / … | signed deltas | `actual − predicted` |
| `evaluatedAt` | Instant | When evaluation ran |

### Notes

- API: `POST /api/v1/predictions/{id}/evaluate` (idempotent conflict if already evaluated).
- Requires a matching PlayerSeason outcome row; returns 404 until that history exists.

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
- **Club uniqueness** — clubs are unique on case-insensitive `name` + `countryCode` via `nameNormalized`; soft delete suffixes `nameNormalized` so re-creation is allowed. Optional `fbrefId` is also unique when set.
- **Player uniqueness** — active players are unique on `(fullNameNormalized, dateOfBirth, nationality)`; optional `fbrefId` is also unique when set and is the preferred ingest key. Soft delete frees both slots.
- **Prediction is scenario-first** — one API creates a run + prediction + explanations; metrics share one row; explanations are child factor rows. Separate per-metric HTTP endpoints were rejected as unproductized.
- **v0 model is heuristic and replaceable** — `PredictionEngine` + `modelVersion` on the run allow swapping algorithms without changing persistence or API contracts.
- **Historical data window** — product ingest targets the top five European leagues (PL, La Liga, Bundesliga, Serie A, Ligue 1) for seasons **2016/17–2025/26** (see `docs/data-sourcing.md`). No fake demo seed.

## Open Questions

- Whether Club needs a separate legal/sporting brand distinction later.
- Whether estimated DOB (`born` year → 1 July) should later be replaced by exact birth dates from a licensed bio feed.
