import { Box, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { ErrorState } from '@/components/common/ErrorState'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { fetchModelAccuracy, type LeagueAccuracy, type MetricBlock } from '@/services/stats/statsApi'
import { formatNumber } from '@/utils/format'

function MetricLine({ label, block }: { label: string; block: MetricBlock | null | undefined }) {
  if (!block?.n || !block.minutes || !block.goals || !block.assists) {
    return (
      <Typography color="text.secondary" variant="body2">
        {label}: no evaluations yet
      </Typography>
    )
  }
  return (
    <Typography variant="body2">
      {label}: minutes MAE {formatNumber(block.minutes.mae, 0)} · goals MAE{' '}
      {formatNumber(block.goals.mae, 1)} · assists MAE {formatNumber(block.assists.mae, 1)} (n=
      {block.n})
    </Typography>
  )
}

function LeagueCard({ league }: { league: LeagueAccuracy }) {
  const sample = league.samples?.[0]
  return (
    <SurfaceCard>
      <Typography color="primary.main" variant="caption">
        {league.leagueName}
      </Typography>
      <MetricLine label="Accuracy" block={league.metrics} />
      {sample ? (
        <Stack spacing={0.5} sx={{ mt: 1.5 }}>
          <Typography color="text.secondary" variant="body2">
            Example: {sample.player} → {sample.club} ({sample.season}) — predicted{' '}
            {formatNumber(sample.predictedMinutes, 0)} min /{' '}
            {formatNumber(Number(sample.predictedGoals), 1)} G, actual{' '}
            {formatNumber(sample.actualMinutes, 0)} min /{' '}
            {formatNumber(Number(sample.actualGoals), 1)} G
          </Typography>
          {sample.predictionId ? (
            <Typography
              component={RouterLink}
              to={routes.predictionDetail(sample.predictionId)}
              variant="body2"
              sx={{ color: 'primary.main', textDecoration: 'none', width: 'fit-content' }}
            >
              Open prediction
            </Typography>
          ) : null}
        </Stack>
      ) : null}
    </SurfaceCard>
  )
}

export function ModelAccuracySection() {
  const query = useQuery({
    queryKey: queryKeys.stats.modelAccuracy(),
    queryFn: fetchModelAccuracy,
    staleTime: 60_000,
  })

  if (query.isLoading) {
    return (
      <SurfaceCard>
        <Typography color="text.secondary" variant="body2">
          Loading model accuracy…
        </Typography>
      </SurfaceCard>
    )
  }

  if (query.isError) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const data = query.data
  if (!data) {
    return null
  }

  const leagues = Object.values(data.byLeague ?? {}).filter((row) => row.metrics?.n > 0)

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h5">Model accuracy by league</Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
          Backtested club-changers on {data.seasons?.join(', ') || 'completed seasons'} — predicted
          destination-season minutes, goals, and assists vs actual PlayerSeason outcomes (
          {data.modelVersion}, n={data.metrics?.n ?? 0}).
        </Typography>
      </Box>

      <SurfaceCard accent="info">
        <Typography color="primary.main" variant="caption">
          Overall
        </Typography>
        <MetricLine label="All leagues" block={data.metrics} />
      </SurfaceCard>

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))', lg: 'repeat(3, minmax(0, 1fr))' },
        }}
      >
        {leagues.map((league) => (
          <LeagueCard key={league.countryCode} league={league} />
        ))}
      </Box>
    </Stack>
  )
}
