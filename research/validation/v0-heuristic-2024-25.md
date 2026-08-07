# Prediction validation — 2024/25

Generated: `2026-08-07T14:22:45.331064+00:00`
Model: `v0-heuristic`
Sample size: **128** evaluated predictions

## Selection

- Min actual minutes: `900`
- Require prior club change: `True`
- Limit: `150`

## Error metrics (actual − predicted)

| Metric | MAE | RMSE | Bias |
|--------|-----|------|------|
| Minutes | 968.01 | 1185.76 | 881.63 |
| Goals | 1.988 | 3.382 | 1.391 |
| Assists | 1.373 | 2.051 | 0.932 |
| xG | 0.0 | 0.0 | — |
| xA | 0.0 | 0.0 | — |

## Notes

- See [`docs/prediction-validation.md`](../../docs/prediction-validation.md) for methodology.
- Negative bias means the model over-predicted on average.
