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
- Packages are organized by feature (`player`, `club`, `manager`, `health`) with shared code in `common`, `domain`, and `config`
- New modules (Season, Tournament, …) should follow the same feature-package layout
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

Nationality and club country codes use FIFA association codes (`ENG`, `GER`, `NED`), not ISO 3166-1.

Identity deletes are soft (`deleted_at`); list/get ignore deleted rows.

## Tests

```bash
./gradlew test
```
