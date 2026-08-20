# Research

Reserved for methodology notes, literature reviews, source assessments, and domain research. Research should record source attribution and licensing constraints.

## Validation artifacts

Season backtests live under [`validation/`](validation/). Methodology: [`docs/prediction-validation.md`](../docs/prediction-validation.md).

Latest published USP artifact: [`validation/latest.json`](validation/latest.json) (overall + by-league MAE for minutes/goals/assists). Served in-product via `GET /api/v1/stats/model-accuracy`.

| Artifact | Model | Notes |
|----------|-------|-------|
| `validation/v0-heuristic-2024-25.*` | `v0-heuristic` | First published baseline |
| `validation/v0.1-heuristic-2024-25.*` | `v0.1-heuristic` | Softer competition + weighted recent minutes |
| `validation/v0.2-heuristic-2024-25.*` | `v0.2-heuristic` | GK starter/backup minutes pathway |
