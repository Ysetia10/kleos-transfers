#!/usr/bin/env bash
# Creates Kleos Transfers roadmap milestones, labels, and issues.
# Requires: gh auth login
set -euo pipefail

REPO="${REPO:-Ysetia10/kleos-transfers}"

create_label() {
  local name="$1" color="$2" description="$3"
  gh label create "$name" --repo "$REPO" --color "$color" --description "$description" 2>/dev/null \
    || gh label edit "$name" --repo "$REPO" --color "$color" --description "$description"
}

create_milestone() {
  local title="$1" description="$2"
  if ! gh api "repos/${REPO}/milestones" --jq '.[].title' | grep -Fxq "$title"; then
    gh api "repos/${REPO}/milestones" -f title="$title" -f description="$description" -f state=open >/dev/null
  fi
}

issue() {
  local title="$1" milestone="$2" labels="$3"
  local body
  body="$(cat)"
  # Skip if an open issue with the same title already exists
  if gh issue list --repo "$REPO" --state all --search "\"${title}\" in:title" --json title --jq '.[].title' | grep -Fxq "$title"; then
    echo "Skip existing: $title"
    return 0
  fi
  gh issue create --repo "$REPO" --title "$title" --milestone "$milestone" --label "$labels" --body "$body" >/dev/null
  echo "Created: $title"
}

echo "Configuring labels..."
create_label "identity" "1f6feb" "Identity-layer domain work"
create_label "historical" "8957e5" "Historical-layer domain work"
create_label "prediction" "d4a72c" "Prediction engine work"
create_label "frontend" "bf8700" "Frontend / UX work"
create_label "infrastructure" "6e7781" "Build, CI, tooling, ops"
create_label "documentation" "0e8a16" "Docs and research notes"
create_label "good-first-issue" "7057ff" "Small, well-scoped starter task"
create_label "enhancement" "a2eeef" "New feature or improvement"
create_label "bug" "d73a4a" "Something is not working"

echo "Configuring milestones..."
create_milestone "v0.2 — Identity Layer" "Permanent identity entities: Club, Manager, Season, Tournament (Player done)."
create_milestone "v0.3 — Historical Layer" "Season-scoped facts: PlayerSeason, ClubSeason, Transfer, Contract, Injury, ManagerSeason."
create_milestone "v0.4 — Prediction Engine" "Explainable adaptation predictions: minutes → goals → assists → market value → compatibility."
create_milestone "v0.5 — Frontend Features" "Prediction form, player/club pages, dashboard, prediction results."
create_milestone "v1.0 — Public Release" "Polish, validation against seasons, docs, open-source release readiness."

echo "Creating issues..."

# --- Infrastructure / setup ---
issue "[Infra] Restructure backend packages by feature before Club" "v0.2 — Identity Layer" "infrastructure,enhancement,good-first-issue" <<'EOF'
## Summary
Move from flat package-by-layer (`controller/`, `service/`, …) to package-by-feature (`player/`, shared `common/`) so Club/Manager do not explode shared packages.

## Scope
- [ ] Introduce `player` feature package (controller, service, dto, mapper, repository, entity)
- [ ] Keep shared code in `common` / `domain` (BaseEntity, exceptions, enums, nationality codes)
- [ ] Update imports and tests; no API behavior change

## Out of scope
- Clean Architecture / hexagonal rewrite
- New endpoints

## Acceptance criteria
- [ ] `./gradlew test` passes
- [ ] Public API paths unchanged
- [ ] README notes the package convention for new modules
EOF

issue "[Infra] Decide identity entity delete strategy (soft vs hard)" "v0.2 — Identity Layer" "infrastructure,documentation,good-first-issue" <<'EOF'
## Summary
Document and implement a deliberate lifecycle for identity entities before historical foreign keys appear.

## Scope
- [ ] Decide soft-delete vs hard-delete vs archive for Player/Club/Manager
- [ ] Record decision in `docs/domain-model.md` Open Questions
- [ ] If soft-delete: add `deletedAt` (or equivalent) via Flyway + filter on reads
- [ ] If hard-delete: document cascade rules and forbid delete once historical rows exist

## Acceptance criteria
- [ ] Written decision with rationale
- [ ] API behavior matches the decision
- [ ] Tests cover the chosen path
EOF

issue "[Infra] Add OpenAPI / SpringDoc for /api/v1" "v0.2 — Identity Layer" "infrastructure,enhancement,documentation" <<'EOF'
## Summary
Expose machine-readable API docs early so frontend and future modules share one contract.

## Scope
- [ ] Add SpringDoc OpenAPI dependency
- [ ] Document Player + Health endpoints
- [ ] Link Swagger UI from backend README

## Acceptance criteria
- [ ] OpenAPI JSON available locally
- [ ] Player create/list/get/update schemas documented
EOF

issue "[Infra] Add Testcontainers PostgreSQL for integration tests" "v0.3 — Historical Layer" "infrastructure,enhancement" <<'EOF'
## Summary
Replace H2-backed integration tests with PostgreSQL via Testcontainers so CHECK constraints and types match production.

## Scope
- [ ] Add Testcontainers Postgres
- [ ] Run Player (and future) integration tests against real Postgres
- [ ] Keep unit tests fast/local without containers where possible

## Acceptance criteria
- [ ] CI runs container-based integration tests
- [ ] Flyway migrations applied against Postgres in tests
EOF

# --- Identity layer ---
issue "[Identity] Implement Club module" "v0.2 — Identity Layer" "identity,enhancement" <<'EOF'
## Summary
Add Club as a permanent identity entity. No seasonal league, squad, or performance data.

## Scope
- [ ] Entity, Flyway migration, repository, service, controller, DTOs, mapper, validation, tests
- [ ] Fields justified for identity only (e.g. name, short name, country/association code, founded year — finalize in issue discussion)
- [ ] API under `/api/v1/clubs` with pagination
- [ ] Update `docs/domain-model.md`

## Out of scope
- ClubSeason, transfers, stadium capacity changes over time, market value

## Acceptance criteria
- [ ] Follows Player module patterns
- [ ] No historical fields on Club
- [ ] Tests cover happy path + validation + 404
EOF

issue "[Identity] Implement Manager module" "v0.2 — Identity Layer" "identity,enhancement" <<'EOF'
## Summary
Add Manager permanent identity (person), separate from ManagerSeason club appointments.

## Scope
- [ ] Full vertical slice like Player
- [ ] Identity-only attributes (name, DOB, nationality, …)
- [ ] `/api/v1/managers` + docs + tests

## Out of scope
- Current club, tactics, win rate, season appointments

## Acceptance criteria
- [ ] Identity/historical boundary respected
- [ ] Domain model doc updated
EOF

issue "[Identity] Implement Season module" "v0.2 — Identity Layer" "identity,enhancement" <<'EOF'
## Summary
Model competition seasons (e.g. 2025/26) as identity records referenced by historical entities.

## Scope
- [ ] Decide season key model (label, start/end dates, hemisphere/calendar rules)
- [ ] Full CRUD (or create/read/update) API under `/api/v1/seasons`
- [ ] Flyway + tests + domain-model update

## Out of scope
- Fixtures, standings, player stats

## Acceptance criteria
- [ ] Unique season identity suitable for FKs from PlayerSeason/Transfer
- [ ] Documented temporal boundary rules
EOF

issue "[Identity] Implement Tournament module" "v0.2 — Identity Layer" "identity,enhancement" <<'EOF'
## Summary
Model competitions (Premier League, Champions League, …) as identity.

## Scope
- [ ] Tournament entity + API `/api/v1/tournaments`
- [ ] Identity attributes only (name, confederation/region, type)
- [ ] Docs + tests

## Out of scope
- Season editions, tables, fixtures

## Acceptance criteria
- [ ] Ready to be referenced by ClubSeason / historical competition rows
EOF

# --- Historical layer ---
issue "[Historical] Implement PlayerSeason" "v0.3 — Historical Layer" "historical,enhancement" <<'EOF'
## Summary
Season-scoped player performance and context (minutes, goals, assists, xG, xA, club, positions played).

## Scope
- [ ] Entity linking Player + Season (+ Club)
- [ ] Stats fields with business justification
- [ ] API + migration + tests
- [ ] Domain model finalized

## Out of scope
- Prediction outputs
- Permanent identity fields duplicated from Player

## Acceptance criteria
- [ ] No identity pollution on Player
- [ ] Unique constraint (player, season, club) or documented alternative
EOF

issue "[Historical] Implement ClubSeason" "v0.3 — Historical Layer" "historical,enhancement" <<'EOF'
## Summary
Season-scoped club context: league, manager appointment link, squad depth signals, competition participation.

## Scope
- [ ] Entity linking Club + Season (+ Tournament/league)
- [ ] Fields needed later for adaptation predictions
- [ ] API + migration + tests + docs

## Out of scope
- Match-by-match results engine
EOF

issue "[Historical] Implement Transfer" "v0.3 — Historical Layer" "historical,enhancement" <<'EOF'
## Summary
Record a player moving between clubs (or free agent) with fee/date/season context.

## Scope
- [ ] Transfer entity (player, from/to club, date, fee, type)
- [ ] API + migration + tests
- [ ] Links usable by prediction “what-if” scenarios later

## Out of scope
- Prediction generation on create
EOF

issue "[Historical] Implement Contract" "v0.3 — Historical Layer" "historical,enhancement" <<'EOF'
## Summary
Player–club contract windows used as prediction context (expiry pressure, renewals).

## Scope
- [ ] Contract entity + API + migration + tests
- [ ] Document relationship to Transfer

## Out of scope
- Wage negotiation simulation
EOF

issue "[Historical] Implement Injury" "v0.3 — Historical Layer" "historical,enhancement" <<'EOF'
## Summary
Injury history as adaptation/risk context for minutes and availability predictions.

## Scope
- [ ] Injury entity (player, dates, type/severity, days out)
- [ ] API + migration + tests

## Out of scope
- Medical ML models
EOF

issue "[Historical] Implement ManagerSeason" "v0.3 — Historical Layer" "historical,enhancement" <<'EOF'
## Summary
Manager appointment at a club for a season (tactical style hooks later).

## Scope
- [ ] ManagerSeason linking Manager + Club + Season
- [ ] Placeholder fields for philosophy/style (only if justified)
- [ ] API + migration + tests + docs
EOF

# --- Prediction layer ---
issue "[Prediction] PredictionRun + Prediction entity foundation" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Persist Kleos prediction outputs and runs separately from identity/historical data.

## Scope
- [ ] `PredictionRun` (inputs snapshot, model version, timestamps)
- [ ] `Prediction` (player, target club, season, metrics, confidence)
- [ ] Flyway + APIs to create/read runs
- [ ] Domain model update

## Out of scope
- Actual scoring algorithms (follow-up issues)
EOF

issue "[Prediction] Minutes prediction service" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
First explainable predictor: expected minutes after transfer.

## Scope
- [ ] Service interface + deterministic v0 heuristic or baseline model
- [ ] Explanation factors recorded (squad competition, age, injury, etc.)
- [ ] Unit tests with fixed fixtures

## Acceptance criteria
- [ ] Controllers stay thin
- [ ] Output includes confidence + explanation hooks
- [ ] No black-box-only result
EOF

issue "[Prediction] Goals prediction service" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Predict goals given expected minutes and contextual fit factors.

## Scope
- [ ] Depends on minutes prediction output
- [ ] Explainable factors (position, xG history, league difficulty placeholders)
- [ ] Tests + evaluation hook stubs
EOF

issue "[Prediction] Assists prediction service" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Predict assists / creative output after transfer.

## Scope
- [ ] Service + explanation factors
- [ ] Tests
- [ ] Wire into Prediction aggregate
EOF

issue "[Prediction] xG and xA prediction" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Predict expected goals and expected assists alongside counting stats.

## Scope
- [ ] xG / xA fields on Prediction
- [ ] Baseline explainable estimators
- [ ] Tests
EOF

issue "[Prediction] Market value after one season" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Estimate end-of-season market value given predicted performance and age/contract context.

## Scope
- [ ] Service + explanation
- [ ] Persist on Prediction
- [ ] Tests with synthetic cases
EOF

issue "[Prediction] Compatibility + confidence scores" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Produce transfer compatibility and overall confidence scores with factor breakdowns.

## Scope
- [ ] Scoring rules documented
- [ ] Factor list persisted for UI explanations
- [ ] Tests for bounds (0–100 or chosen scale)
EOF

issue "[Prediction] PredictionExplanation + evaluation entities" "v0.4 — Prediction Engine" "prediction,enhancement" <<'EOF'
## Summary
Store human-readable factor explanations and later compare predictions to real outcomes.

## Scope
- [ ] `PredictionExplanation` rows per factor
- [ ] `PredictionEvaluation` for post-season accuracy
- [ ] APIs to read explanations; evaluation can be internal-first
EOF

# --- Frontend ---
issue "[Frontend] Typed Player API client + Players list/detail pages" "v0.5 — Frontend Features" "frontend,enhancement" <<'EOF'
## Summary
Wire frontend to `/api/v1/players` with typed services and real pages (not placeholders).

## Scope
- [ ] `services/player` client + query hooks using `queryKeys`
- [ ] List page with pagination
- [ ] Detail page by id
- [ ] Loading / empty / error feedback components

## Acceptance criteria
- [ ] Uses shared Axios error model
- [ ] No fake football data hardcoded as production content
EOF

issue "[Frontend] Club pages (list + detail)" "v0.5 — Frontend Features" "frontend,enhancement" <<'EOF'
## Summary
Club UI after Club API exists.

## Scope
- [ ] Typed club client
- [ ] List + detail routes
- [ ] Nav already points to `/clubs`
EOF

issue "[Frontend] Prediction form (player → club scenario)" "v0.5 — Frontend Features" "frontend,enhancement" <<'EOF'
## Summary
Form to request a transfer prediction (player, target club, season).

## Scope
- [ ] React Hook Form + validation (zod recommended)
- [ ] Submit to prediction API
- [ ] Shared form field components

## Out of scope
- Chart-heavy results (follow-up)
EOF

issue "[Frontend] Prediction results view with explanations" "v0.5 — Frontend Features" "frontend,enhancement" <<'EOF'
## Summary
Display minutes/goals/assists/xG/xA/value/compatibility/confidence and factor explanations.

## Scope
- [ ] Results layout
- [ ] Explanation list (not black-box)
- [ ] Optional Recharts only where it clarifies trends
EOF

issue "[Frontend] Dashboard shell with recent predictions" "v0.5 — Frontend Features" "frontend,enhancement" <<'EOF'
## Summary
Dashboard summarizing recent prediction runs and key platform entry points.

## Scope
- [ ] Replace placeholder Dashboard page
- [ ] Recent runs list
- [ ] Links to predict / players / clubs
EOF

issue "[Frontend] Mobile navigation drawer" "v0.5 — Frontend Features" "frontend,enhancement,good-first-issue" <<'EOF'
## Summary
Replace wrapping nav links with a drawer/menu on small viewports.

## Scope
- [ ] MUI Drawer or temporary menu for `xs`
- [ ] Keep desktop horizontal nav
- [ ] Accessibility: focus, aria labels
EOF

# --- v1.0 / research ---
issue "[Docs] Architecture Decision Records (ADR) folder" "v1.0 — Public Release" "documentation,good-first-issue" <<'EOF'
## Summary
Start lightweight ADRs for nationality codes, delete strategy, prediction versioning, data licensing.

## Scope
- [ ] `docs/adr/` template
- [ ] First ADRs for decisions already made (FIFA nationality, API v1)
EOF

issue "[Docs] Data sourcing and licensing policy" "v1.0 — Public Release" "documentation" <<'EOF'
## Summary
Document ethical data sourcing, licensing, and redistribution rules before ingestion work.

## Scope
- [ ] Policy doc under `docs/` or `research/`
- [ ] Allowed sources checklist
- [ ] Attribution requirements
EOF

issue "[Release] Seed script for demo identity data" "v1.0 — Public Release" "infrastructure,enhancement" <<'EOF'
## Summary
Reproducible demo seed (players/clubs) for local UX and screenshots — not production stats pipelines.

## Scope
- [ ] Script under `scripts/`
- [ ] Idempotent or clearly destructive with confirmation
- [ ] Documented in README
EOF

issue "[Release] Prediction validation against a completed season" "v1.0 — Public Release" "prediction,documentation" <<'EOF'
## Summary
Compare stored predictions to real end-of-season outcomes; publish error metrics.

## Scope
- [ ] Evaluation job or service using `PredictionEvaluation`
- [ ] Document methodology for blogs/papers later
- [ ] Product remains primary; research artifact is secondary
EOF

issue "[Release] Public README polish and contribution guide" "v1.0 — Public Release" "documentation,good-first-issue" <<'EOF'
## Summary
Prepare open-source onboarding: CONTRIBUTING, code of conduct optional, clear architecture map.

## Scope
- [ ] CONTRIBUTING.md
- [ ] Refresh root README feature status
- [ ] Link milestones/issues for newcomers
EOF

echo "Done. View issues: gh issue list --repo $REPO"
