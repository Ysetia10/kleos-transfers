# Contributing to Kleos Transfers

Thanks for helping improve an open-source, explainable football transfer prediction platform.

## Before you start

1. Read the [README](README.md) for stack and local setup.
2. Skim [`docs/domain-model.md`](docs/domain-model.md) (identity / historical / prediction layers).
3. Check [`docs/adr/`](docs/adr/) for settled architecture decisions before proposing alternatives.
4. If you touch ingest or publish numbers derived from FBref, read [`docs/data-sourcing.md`](docs/data-sourcing.md).
5. Prefer an existing GitHub issue. Comment if you want to claim it.

## Local development

### Backend

- Java 21, PostgreSQL, Gradle wrapper in `backend/`
- Configure DB via `.env` (see `.env.example`)
- `cd backend && ./gradlew test && ./gradlew bootRun`
- API base: `http://localhost:8080/api/v1`

### Frontend

- Node 20+ recommended
- `cd frontend && cp .env.example .env && npm install && npm run dev`
- Vite: `http://localhost:5173` or `http://127.0.0.1:5173` (both allowed by CORS defaults)

### Historical data

Do **not** commit scraped dumps. Use `scripts/ingest_fbref_pl_laliga.py` (headless by default) against a running API. Start with `--dry-run --seasons 2024/25`.

### Prediction validation

```bash
./scripts/validate_predictions_season.py --season 2024/25 --dry-run
./scripts/validate_predictions_season.py --season 2024/25 --limit 150
```

Methodology: [`docs/prediction-validation.md`](docs/prediction-validation.md).

## Pull requests

- Keep PRs focused (one concern per PR when practical).
- Match existing package-by-feature layout in `backend/src/main/java/com/kleos/transfers/`.
- Add or update tests for behavior changes (`./gradlew test`; Docker required for integration tests).
- Frontend: `npm run lint` / `npm run build` when touching UI. New nav items belong in `navigationItems` (desktop + mobile drawer).
- Do not commit secrets, `.env`, or `~/soccerdata` caches.

## Issue labels (quick map)

| Label | Use for |
|-------|---------|
| `prediction` | Engine, evaluation, model versions |
| `historical` | Season-scoped facts / ingest |
| `frontend` | React UI |
| `infrastructure` | CI, tooling, OpenAPI, Testcontainers |
| `documentation` | Docs / research notes |
| `good-first-issue` | Small, well-scoped starters |

## Code of conduct

Be respectful and constructive. Harassment or bad-faith behavior is not welcome.
