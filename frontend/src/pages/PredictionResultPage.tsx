import { Box, Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { SquadTable } from '@/components/home/SquadTable'
import { PitchLineup } from '@/components/squad/PitchLineup'
import { ActualOutcomeCard } from '@/components/prediction/ActualOutcomeCard'
import { CompatibilityBreakdownChips } from '@/components/prediction/CompatibilityBreakdownChips'
import { ExplanationList } from '@/components/prediction/ExplanationList'
import { MetricGrid } from '@/components/prediction/MetricGrid'
import { ScoreMeter } from '@/components/prediction/ScoreMeter'
import { homePredictPath } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClubSquad } from '@/services/club/squadApi'
import { getPrediction } from '@/services/prediction/predictionApi'
import { downloadPredictionBrief } from '@/utils/exportPredictionBrief'
import { formatNumber } from '@/utils/format'

export function PredictionResultPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: queryKeys.predictions.detail(id),
    queryFn: () => getPrediction(id),
    enabled: !!id,
  })

  const prediction = query.data
  const squadQuery = useQuery({
    queryKey: queryKeys.clubs.squad(
      prediction?.targetClubId ?? '',
      prediction?.seasonId ?? '',
    ),
    queryFn: () => getClubSquad(prediction!.targetClubId, prediction!.seasonId),
    enabled: !!prediction?.targetClubId && !!prediction?.seasonId,
  })

  if (query.isLoading) {
    return <LoadingState label="Loading prediction…" />
  }
  if (query.isError || !prediction) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const confidence = Number(prediction.confidenceScore)
  const confidenceLabel =
    confidence >= 70 ? 'High signal' : confidence >= 45 ? 'Moderate signal' : 'Thin signal'

  return (
    <Stack spacing={4}>
      <PageHeader
        actions={
          <>
            <Button onClick={() => downloadPredictionBrief(prediction)} variant="outlined">
              Export brief
            </Button>
            <Button component={RouterLink} to={homePredictPath()} variant="outlined">
              New prediction
            </Button>
          </>
        }
        description={`${prediction.playerName} → ${prediction.targetClubName} · ${prediction.seasonLabel}`}
        eyebrow="Prediction workspace"
        title="Prediction"
      />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 0.9fr) minmax(0, 1.1fr) minmax(0, 1fr)' },
        }}
      >
        <Stack spacing={2}>
          <SurfaceCard>
            <Typography color="text.secondary" variant="caption">
              Player
            </Typography>
            <Typography sx={{ mt: 1 }} variant="h3">
              {prediction.playerName}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
              Target · {prediction.targetClubName}
            </Typography>
          </SurfaceCard>
          <SurfaceCard accent="positive">
            <Typography color="text.secondary" variant="caption">
              Model confidence
            </Typography>
            <Typography sx={{ mt: 1 }} variant="h1">
              {formatNumber(confidence, 0)}%
            </Typography>
            <Typography color="success.main" sx={{ mt: 0.5 }} variant="body2">
              {confidenceLabel}
            </Typography>
            <Box sx={{ mt: 2 }}>
              <ScoreMeter label="Confidence" value={confidence} />
            </Box>
          </SurfaceCard>
          <SurfaceCard>
            <ScoreMeter
              helper="Aggregate of system, role, tempo, league, and manager fit."
              label="Compatibility"
              value={Number(prediction.compatibilityScore)}
            />
            <Box sx={{ mt: 2 }}>
              <Typography color="text.secondary" sx={{ mb: 1 }} variant="caption">
                Fit dimensions
              </Typography>
              <CompatibilityBreakdownChips breakdown={prediction.compatibilityBreakdown} />
            </Box>
          </SurfaceCard>
        </Stack>

        <Stack spacing={2}>
          <SurfaceCard>
            <Typography color="text.secondary" variant="caption">
              First-season estimate
            </Typography>
            <Typography sx={{ mt: 1, mb: 2 }} variant="h3">
              Projected output
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
              minutesHigh={prediction.predictedMinutesHigh}
              minutesLow={prediction.predictedMinutesLow}
            />
          </SurfaceCard>
          {prediction.evaluation ? (
            <ActualOutcomeCard evaluation={prediction.evaluation} prediction={prediction} />
          ) : null}
        </Stack>

        <SurfaceCard>
          <Typography color="text.secondary" variant="caption">
            Contextual signals
          </Typography>
          <Typography sx={{ mt: 1, mb: 2 }} variant="h3">
            Why this result
          </Typography>
          <ExplanationList explanations={prediction.explanations} />
        </SurfaceCard>
      </Box>

      <Stack spacing={2}>
        <Typography variant="h3">
          {prediction.targetClubName} squad · {prediction.seasonLabel}
        </Typography>
        <Typography color="text.secondary" variant="body2">
          For upcoming seasons this is the prior roster updated with confirmed/announced transfers.
          Minutes reflect the prior campaign until {prediction.seasonLabel} actuals exist.
        </Typography>
        {!squadQuery.isLoading && !squadQuery.isError ? (
          <PitchLineup squad={squadQuery.data} />
        ) : null}
        <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
          <SquadTable
            error={squadQuery.error}
            isError={squadQuery.isError}
            isLoading={squadQuery.isLoading}
            onRetry={() => void squadQuery.refetch()}
            squad={squadQuery.data}
          />
        </SurfaceCard>
      </Stack>
    </Stack>
  )
}
