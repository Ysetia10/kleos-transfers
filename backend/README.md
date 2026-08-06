# Backend

Spring Boot 3 backend for Kleos Transfers.

## Technology

- Java 21 and Gradle
- Spring Web, Validation, Data JPA, Actuator, and Lombok
- PostgreSQL and Flyway
- H2 for automated tests (Flyway migrations applied)

## Architecture notes

- Controllers stay thin; services own use cases
- DTOs isolate the API from JPA entities
- Packages are organized by feature (`player`, `club`, `manager`, `season`, `tournament`, `clubseason`, `managerseason`, `playerseason`, `transfer`, `contract`, `injury`, `prediction`, `health`) with shared code in `common`, `domain`, and `config`
- New modules should follow the same feature-package layout
- Identity entities extend `common.entity.IdentityEntity` for the shared UUID key, auditing, and soft delete
- Shared vocabulary lives in `com.kleos.transfers.domain`
- Flyway owns schema; JPA uses `ddl-auto: validate`
- Public HTTP API is versioned under `/api/v1`

## Run locally

Provide a reachable PostgreSQL database, then:

```bash
./gradlew bootRun
```

Configuration uses environment-variable placeholders. See [`../.env.example`](../.env.example).

Optional CORS origins (comma-separated):

```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

## Current endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/v1/health` | Application health payload |
| POST | `/api/v1/players` | Create player identity |
| GET | `/api/v1/players` | List players (paginated) |
| GET | `/api/v1/players/{id}` | Get player by id |
| PUT | `/api/v1/players/{id}` | Replace player identity |
| DELETE | `/api/v1/players/{id}` | Soft-delete player identity |
| POST | `/api/v1/clubs` | Create club identity |
| GET | `/api/v1/clubs` | List clubs (paginated) |
| GET | `/api/v1/clubs/{id}` | Get club by id |
| PUT | `/api/v1/clubs/{id}` | Replace club identity |
| DELETE | `/api/v1/clubs/{id}` | Soft-delete club identity |
| POST | `/api/v1/managers` | Create manager identity |
| GET | `/api/v1/managers` | List managers (paginated) |
| GET | `/api/v1/managers/{id}` | Get manager by id |
| PUT | `/api/v1/managers/{id}` | Replace manager identity |
| DELETE | `/api/v1/managers/{id}` | Soft-delete manager identity |
| POST | `/api/v1/players/bulk` | Import up to 500 players |
| POST | `/api/v1/clubs/bulk` | Import up to 500 clubs |
| POST | `/api/v1/managers/bulk` | Import up to 500 managers |
| POST | `/api/v1/seasons` | Create season identity |
| GET | `/api/v1/seasons` | List seasons (paginated) |
| GET | `/api/v1/seasons/{id}` | Get season by id |
| PUT | `/api/v1/seasons/{id}` | Replace season identity |
| DELETE | `/api/v1/seasons/{id}` | Soft-delete season identity |
| POST | `/api/v1/seasons/bulk` | Import up to 500 seasons |
| POST | `/api/v1/tournaments` | Create tournament identity |
| GET | `/api/v1/tournaments` | List tournaments (paginated) |
| GET | `/api/v1/tournaments/{id}` | Get tournament by id |
| PUT | `/api/v1/tournaments/{id}` | Replace tournament identity |
| DELETE | `/api/v1/tournaments/{id}` | Soft-delete tournament identity |
| POST | `/api/v1/tournaments/bulk` | Import up to 500 tournaments |
| POST | `/api/v1/club-seasons` | Create club-season record |
| GET | `/api/v1/club-seasons` | List club-seasons (paginated) |
| GET | `/api/v1/club-seasons/{id}` | Get club-season by id |
| PUT | `/api/v1/club-seasons/{id}` | Replace club-season links |
| DELETE | `/api/v1/club-seasons/{id}` | Soft-delete club-season |
| POST | `/api/v1/club-seasons/bulk` | Import up to 500 club-seasons |
| POST | `/api/v1/manager-seasons` | Create manager-season appointment |
| GET | `/api/v1/manager-seasons` | List manager-seasons (paginated) |
| GET | `/api/v1/manager-seasons/{id}` | Get manager-season by id |
| PUT | `/api/v1/manager-seasons/{id}` | Replace manager-season links |
| DELETE | `/api/v1/manager-seasons/{id}` | Soft-delete manager-season |
| POST | `/api/v1/manager-seasons/bulk` | Import up to 500 manager-seasons |
| POST | `/api/v1/player-seasons` | Create player-season performance |
| GET | `/api/v1/player-seasons` | List player-seasons (paginated) |
| GET | `/api/v1/player-seasons/{id}` | Get player-season by id |
| PUT | `/api/v1/player-seasons/{id}` | Replace player-season |
| DELETE | `/api/v1/player-seasons/{id}` | Soft-delete player-season |
| POST | `/api/v1/player-seasons/bulk` | Import up to 500 player-seasons |
| POST | `/api/v1/transfers` | Create transfer record |
| GET | `/api/v1/transfers` | List transfers (paginated) |
| GET | `/api/v1/transfers/{id}` | Get transfer by id |
| PUT | `/api/v1/transfers/{id}` | Replace transfer |
| DELETE | `/api/v1/transfers/{id}` | Soft-delete transfer |
| POST | `/api/v1/transfers/bulk` | Import up to 500 transfers |
| POST | `/api/v1/contracts` | Create contract record |
| GET | `/api/v1/contracts` | List contracts (paginated) |
| GET | `/api/v1/contracts/{id}` | Get contract by id |
| PUT | `/api/v1/contracts/{id}` | Replace contract |
| DELETE | `/api/v1/contracts/{id}` | Soft-delete contract |
| POST | `/api/v1/contracts/bulk` | Import up to 500 contracts |
| POST | `/api/v1/injuries` | Create injury spell |
| GET | `/api/v1/injuries` | List injuries (paginated) |
| GET | `/api/v1/injuries/{id}` | Get injury by id |
| PUT | `/api/v1/injuries/{id}` | Replace injury |
| DELETE | `/api/v1/injuries/{id}` | Soft-delete injury |
| POST | `/api/v1/injuries/bulk` | Import up to 500 injuries |

Nationality and club/tournament country codes use FIFA association codes (`ENG`, `GER`, `NED`), not ISO 3166-1.

ClubSeason links `clubId` + `seasonId` + primary `tournamentId`. One active row per club per season.

ManagerSeason links `managerId` + `clubId` + `seasonId`. Unique per manager/club/season; multiple managers at the same club in one season are allowed.

PlayerSeason links `playerId` + `clubId` + `seasonId` with appearances, minutes, goals, assists, xG, xA, and seasonal primary position. Unique per player/club/season.

Transfer links `playerId`, optional `fromClubId` / `toClubId`, `seasonId`, `transferDate`, optional `feeEur`, and `type` (`PERMANENT`, `LOAN`, `FREE`, `LOAN_RETURN`).

Contract links `playerId` + `clubId` with `startDate`, `endDate`, and an optional `releaseClauseEur`. Unique per player/club/start date, so a renewal is a new row rather than an edit. Contracts are not created automatically by a transfer.

Injury links `playerId` with `injuryType`, `severity` (`MINOR`, `MODERATE`, `SEVERE`), `startDate`, and an optional `endDate` (null while the player is still out). Responses derive `daysOut` and `ongoing` from the dates. Injuries are date-ranged, not season-scoped.

### Predictions

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/predictions` | Run a player→club→season what-if (creates run + prediction + explanations) |
| GET | `/api/v1/predictions` | List predictions (paginated) |
| GET | `/api/v1/predictions/{id}` | Get prediction with explanations and optional evaluation |
| POST | `/api/v1/predictions/{id}/evaluate` | Compare against matching PlayerSeason outcome |
| DELETE | `/api/v1/predictions/{id}` | Soft-delete prediction |
| GET | `/api/v1/prediction-runs/{id}` | Get run metadata with nested predictions |

v0 model version is `v0-heuristic`: deterministic minutes/goals/assists/xG/xA/value plus compatibility and confidence scores, each backed by explanation factors.

Season labels are `YYYY/YY` (European) or `YYYY` (calendar year). `endDate` must be after `startDate`.

Tournament `confederation` is one of `UEFA`, `CONMEBOL`, `CONCACAF`, `CAF`, `AFC`, `OFC`, `FIFA`. `type` is `LEAGUE`, `CUP`, or `SUPER_CUP`. `countryCode` is optional (domestic only).

Identity deletes are soft (`deleted_at`); list/get ignore deleted rows.

## Bulk import

`POST /api/v1/{resource}/bulk` takes `{"items": [...]}` of the same payloads the single-create endpoints accept, and always returns `200` with a per-row summary:

```json
{
  "requested": 3,
  "createdCount": 1,
  "skippedCount": 1,
  "failedCount": 1,
  "created": [{ "id": "…", "fullName": "Bukayo Saka" }],
  "skipped": [{ "index": 1, "reference": "Bukayo Saka", "reason": "already exists" }],
  "failed": [{ "index": 2, "reference": "X", "reason": "nationality must be a valid FIFA nationality code" }]
}
```

One bad row never rejects the batch, and re-running an import is safe. Duplicates are matched on a natural key — name, date of birth, and nationality for people; name and country for clubs — so imports are repeatable without creating duplicate identities.

Shared logic lives in `common.bulk`; a new identity module only implements `BulkImportSpec`.

Load CSV files with [`../scripts/import-identities.py`](../scripts/README.md).

## Tests

```bash
./gradlew test
```
