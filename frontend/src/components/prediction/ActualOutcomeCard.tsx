import { Box, Typography } from '@mui/material'
import { Fragment } from 'react'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import type { Evaluation, Prediction } from '@/types/domain'
import { formatNumber } from '@/utils/format'

type ActualOutcomeCardProps = {
  prediction: Prediction
  evaluation: Evaluation
}

function signed(value: number | null, digits = 1): string {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  const n = Number(value)
  const prefix = n > 0 ? '+' : ''
  return `${prefix}${formatNumber(n, digits)}`
}

export function ActualOutcomeCard({ prediction, evaluation }: ActualOutcomeCardProps) {
  const rows = [
    {
      label: 'Minutes',
      predicted: formatNumber(prediction.predictedMinutes),
      actual: evaluation.actualMinutes == null ? '—' : formatNumber(evaluation.actualMinutes),
      error: signed(evaluation.minutesError, 0),
    },
    {
      label: 'Goals',
      predicted: formatNumber(Number(prediction.predictedGoals), 1),
      actual: evaluation.actualGoals == null ? '—' : formatNumber(evaluation.actualGoals, 0),
      error: signed(evaluation.goalsError == null ? null : Number(evaluation.goalsError), 1),
    },
    {
      label: 'Assists',
      predicted: formatNumber(Number(prediction.predictedAssists), 1),
      actual: evaluation.actualAssists == null ? '—' : formatNumber(evaluation.actualAssists, 0),
      error: signed(evaluation.assistsError == null ? null : Number(evaluation.assistsError), 1),
    },
  ]

  return (
    <SurfaceCard accent="info">
      <Typography color="primary.main" variant="caption">
        Actual {prediction.seasonLabel} outcome
      </Typography>
      <Typography sx={{ mt: 1, mb: 1 }} variant="h3">
        Predicted vs actual
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }} variant="body2">
        Error is actual − predicted. Positive means the player outperformed the projection.
      </Typography>
      <Box
        sx={{
          display: 'grid',
          gap: { xs: 1, sm: 1.25 },
          gridTemplateColumns: {
            xs: 'minmax(0, 1.1fr) repeat(3, minmax(0, 1fr))',
            sm: '1.2fr 1fr 1fr 1fr',
          },
          alignItems: 'baseline',
          minWidth: 0,
          overflowX: 'auto',
        }}
      >
        <Typography color="text.secondary" variant="caption">
          Metric
        </Typography>
        <Typography color="text.secondary" variant="caption">
          Pred.
        </Typography>
        <Typography color="text.secondary" variant="caption">
          Act.
        </Typography>
        <Typography color="text.secondary" variant="caption">
          Err.
        </Typography>
        {rows.map((row) => (
          <Fragment key={row.label}>
            <Typography sx={{ overflowWrap: 'anywhere' }} variant="body2">
              {row.label}
            </Typography>
            <Typography variant="body2">{row.predicted}</Typography>
            <Typography variant="body2">{row.actual}</Typography>
            <Typography variant="body2">{row.error}</Typography>
          </Fragment>
        ))}
      </Box>
    </SurfaceCard>
  )
}
