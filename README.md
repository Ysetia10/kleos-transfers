# Kleos Transfers

Kleos Transfers is an open-source, context-aware football transfer prediction platform. Its long-term purpose is to estimate how a player is likely to perform after joining a specific club, before the season begins.

The project focuses on the environment around a transfer—not only a player's historical record. Future analyses will consider factors such as tactical fit, squad competition, managerial approach, fixture demands, league transition, age profile, injury history, and international commitments.

## Vision

Build an accessible and explainable decision-support platform for football transfers. Kleos Transfers should make its conclusions understandable: users should be able to see the positive and negative contextual factors behind each transfer assessment.

## Current status

Version **0.2** — Identity Layer in progress.

Completed:

- Repository, backend, and frontend foundations
- Shared domain foundation (`BaseEntity`, auditing, enums)
- Player, Club, Manager, Season, and Tournament identity modules (API, persistence, validation, tests)
- ClubSeason and ManagerSeason historical modules
- Bulk identity import API + CSV loader script
- Material UI design system and application shell

Next historical entities: PlayerSeason, Transfer, Contract, Injury, …

## High-level architecture

```text
Data sources and research
        |
        v
Database <--> Analytics and evaluation <--> Backend API <--> Frontend
        ^                                      |
        |                                      v
Database schema and migrations           Documentation and operations
```

Domain modeling separates:

1. **Identity** — permanent records (Player, Club, Manager, Season, Tournament)
2. **Historical** — season-scoped facts (PlayerSeason, Transfer, Injury, …)
3. **Prediction** — Kleos intelligence (Prediction, Evaluation, Explanation)

See [`docs/domain-model.md`](docs/domain-model.md).

## Repository structure

```text
backend/    Spring Boot API and persistence
frontend/   React + Vite web application
analytics/  Future analysis and evaluation work
database/   Schema notes and data-access documentation
docs/       Architecture, domain model, and decisions
research/   Research notes and methodology
scripts/    Development automation
.github/    GitHub Actions and repository automation
```

## Getting started

### Backend

Requires Java 21, Gradle, and PostgreSQL.

```bash
cd backend
cp ../.env.example ../.env   # fill DATABASE_* values
./gradlew test
./gradlew bootRun
```

API base path: `http://localhost:8080/api/v1`

- `GET /api/v1/health`
- `GET|POST /api/v1/players` and `GET|PUT|DELETE /api/v1/players/{id}`
- `GET|POST /api/v1/clubs` and `GET|PUT|DELETE /api/v1/clubs/{id}`
- `GET|POST /api/v1/managers` and `GET|PUT|DELETE /api/v1/managers/{id}`
- `GET|POST /api/v1/seasons` and `GET|PUT|DELETE /api/v1/seasons/{id}`
- `GET|POST /api/v1/tournaments` and `GET|PUT|DELETE /api/v1/tournaments/{id}`
- `GET|POST /api/v1/club-seasons` and `GET|PUT|DELETE /api/v1/club-seasons/{id}`
- `GET|POST /api/v1/manager-seasons` and `GET|PUT|DELETE /api/v1/manager-seasons/{id}`
- `POST /api/v1/{players|clubs|managers|seasons|tournaments|club-seasons|manager-seasons}/bulk` for batch imports

### Loading identity data

Creating records one at a time does not scale, so identity data is loaded from CSV:

```bash
./scripts/import-identities.py players scripts/sample-data/players.csv
```

See [`scripts/README.md`](scripts/README.md) for column formats and options.

### Frontend

Requires Node.js 20+.

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

The Vite app defaults to `http://localhost:5173` and expects the API at `http://localhost:8080`.

## License

Kleos Transfers is released under the [MIT License](LICENSE).
