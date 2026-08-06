import { Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { ExplanationList } from '@/components/prediction/ExplanationList'
import { MetricGrid } from '@/components/prediction/MetricGrid'
import { ScoreMeter } from '@/components/prediction/ScoreMeter'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getPrediction } from '@/services/prediction/predictionApi'
import { formatDateTime } from '@/utils/format'

export function PredictionResultPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: queryKeys.predictions.detail(id),
    queryFn: () => getPrediction(id),
    enabled: !!id,
  })

  if (query.isLoading) {
    return <LoadingState label="Loading prediction…" />
  }
  if (query.isError || !query.data) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const prediction = query.data

  return (
    <Stack spacing={5}>
      <PageHeader
        actions={
          <Button component={RouterLink} to={routes.prediction} variant="outlined">
            New prediction
          </Button>
        }
        description={`${prediction.playerName} → ${prediction.targetClubName} · ${prediction.seasonLabel}`}
        title="Prediction result"
      />

      <Stack spacing={1}>
        <Typography color="text.secondary" variant="body2">
          Model {prediction.modelVersion} · run {formatDateTime(prediction.createdAt)}
        </Typography>
      </Stack>

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={4}>
        <Stack spacing={3} sx={{ flex: 1 }}>
          <Typography component="h2" variant="h3">
            Projected season
          </Typography>
          <MetricGrid
            assists={Number(prediction.predictedAssists)}
            goals={Number(prediction.predictedGoals)}
            marketValueEur={
              prediction.predictedMarketValueEur == null
                ? null
                : Number(prediction.predictedMarketValueEur)
            }
            minutes={prediction.predictedMinutes}
            xa={Number(prediction.predictedXa)}
            xg={Number(prediction.predictedXg)}
          />
        </Stack>
        <Stack spacing={3} sx={{ flex: 1, maxWidth: { md: 360 } }}>
          <Typography component="h2" variant="h3">
            Fit scores
          </Typography>
          <ScoreMeter
            helper="How well the move fits age, role, injury, contract, and league context."
            label="Compatibility"
            value={Number(prediction.compatibilityScore)}
          />
          <ScoreMeter
            helper="How complete the historical inputs were for this scenario."
            label="Confidence"
            value={Number(prediction.confidenceScore)}
          />
        </Stack>
      </Stack>

      <Stack spacing={2}>
        <Typography component="h2" variant="h3">
          Why this result
        </Typography>
        <Typography color="text.secondary" variant="body2">
          Every factor below contributed to the projection. Positive factors support the move;
          negative factors pull it down.
        </Typography>
        <ExplanationList explanations={prediction.explanations} />
      </Stack>
    </Stack>
  )
}
