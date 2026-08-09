# Prediction validation methodology

This document describes how Kleos compares transfer predictions (currently **`v0.3-heuristic`**) to observed end-of-season outcomes. It is the research/product checklist behind issue #32 and the calibration loop for issue #38.

**Product loop:** backtest on completed seasons (primary cohort: 2024/25 club-changers; optional `--countries ENG,ESP`) → publish MAE/bias under `research/validation/` → use the same as-of engine to project **upcoming** seasons such as 2026/27 (Season shell via `scripts/ensure_predict_seasons.py`; no evaluate until actuals exist).

## Goal

Estimate how wrong the current engine is on a **completed** season, using the same `Prediction` + `PredictionEvaluation` path the product API exposes — not a separate offline model.

## As-of semantics (no outcome leak)

When creating a prediction for season \(S\), `PredictionContextLoader` loads inputs **as of** \(S\)'s start date:

| Input | Rule |
|-------|------|
| Player history | `PlayerSeason` rows with `season.startDate < S.startDate` only |
| Squad competition | Target club's roster from the **previous** season (not \(S\)) |
| Injuries | From \(S.startDate - 1\) year onward (pre-season context) |
| ClubSeason metadata | Previous season row for the target club when present |

The outcome row for \(S\) is used **only** by `POST /api/v1/predictions/{id}/evaluate`, which writes a `PredictionEvaluation` (`actual − predicted` errors).

## Cohort selection

Default validation cohort for season \(S\):

1. Player has a `PlayerSeason` in \(S\) with at least **900** minutes (regular contributor).
2. Player has at least one prior `PlayerSeason` before \(S\).
3. Optionally (default **on**): the player's **most recent prior club** differs from the club in \(S\) (arrived / returned that season).

Ordered by minutes descending, then capped by `--limit`.

### Known data caveats

- Selecting on **≥900 actual minutes** conditions on survivors who played a lot; minutes MAE will look harsh if the model is cautious.
- Current FBref ingest often stores **xG/xA as 0** when soccerdata tables omit those columns for a league-season. Until that is fixed, xG/xA error metrics may be uninformative (both predicted and actual near zero).

## Procedure

```bash
# Backend must be running against the loaded historical DB
./scripts/validate_predictions_season.py --season 2024/25 --dry-run
./scripts/validate_predictions_season.py --season 2024/25 --limit 200
```

For each candidate the job:

1. `POST /api/v1/predictions` with `note=backtest:{season}:{n}`
2. `POST /api/v1/predictions/{id}/evaluate`
3. Aggregates MAE, RMSE, and mean bias for minutes / goals / assists / xG / xA

Artifacts land under `research/validation/` (JSON + short Markdown summary).

## Metrics

- **MAE** — mean absolute error; primary readability metric for blogs/UI copy.
- **RMSE** — penalizes large misses (e.g. star minutes collapses).
- **Bias** — mean `(actual − predicted)`; negative ⇒ systematic over-prediction.

Market value and compatibility/confidence scores are **not** scored here (no ground-truth series in the current historical layer).

## What this is not

- Not a causal claim about transfer quality.
- Not a claim that v0 is production-grade; it is a **baseline** to beat with stronger models.
- Not a license to republish FBref raw tables — only derived error aggregates and methodology.

## Attribution

Historical outcomes come from the Kleos DB loaded via the FBref ingest path. Credit **FBref / Sports Reference** and `soccerdata` when publishing numbers derived from that load. See [`data-sourcing.md`](data-sourcing.md).
