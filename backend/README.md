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
- Shared vocabulary lives in `com.kleos.transfers.domain`
- Flyway owns schema; JPA uses `ddl-auto: validate`
- Public HTTP API is versioned under `/api/v1`

## Run locally

Provide a reachable PostgreSQL database, then:

```bash
gradle bootRun
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

Nationality uses FIFA association codes (`ENG`, `GER`, `NED`), not ISO 3166-1.

## Tests

```bash
gradle test
```
