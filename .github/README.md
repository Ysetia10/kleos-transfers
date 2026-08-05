# GitHub configuration

Contains continuous-integration workflows, issue templates, and repository automation.

- `.github/workflows/ci.yml` — backend (`./gradlew test`) and frontend build/lint on pushes and PRs to `main`
- `.github/ISSUE_TEMPLATE/` — Feature and Bug issue forms
- Roadmap issues/milestones are created via `scripts/create-roadmap-issues.sh`
