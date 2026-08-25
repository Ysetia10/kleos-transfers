import { Box, Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { ScenarioComparisonSection } from '@/components/player/ScenarioComparisonSection'
import { InjuryHistorySection } from '@/components/player/InjuryHistorySection'
import { TransferBadge } from '@/components/transfer/TransferBadge'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getPlayer } from '@/services/player/playerApi'
import { formatFootballCountry } from '@/utils/footballCountry'
import { formatAge, formatDateOfBirth } from '@/utils/format'

export function PlayerDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: queryKeys.players.detail(id),
    queryFn: () => getPlayer(id),
    enabled: !!id,
  })

  if (query.isLoading) {
    return <LoadingState />
  }
  if (query.isError || !query.data) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const player = query.data
  const stats = [
    { label: 'Age', value: formatAge(player.age) },
    { label: 'Height', value: player.heightCm == null ? '—' : `${player.heightCm} cm` },
    { label: 'Preferred foot', value: player.preferredFoot ?? '—' },
    { label: 'Position', value: player.primaryPosition },
  ]

  return (
    <Stack spacing={3}>
      <PageHeader
        actions={
          <Button
            component={RouterLink}
            to={homePredictPath({ playerId: player.id })}
            variant="contained"
          >
            Simulate transfer
          </Button>
        }
        description={
          <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.75, flexWrap: 'wrap' }}>
            <span>
              {player.primaryPosition} · {formatFootballCountry(player.nationality)}
              {player.latestClubName ? ' · ' : ''}
            </span>
            {player.latestClubName && player.latestTransfer ? (
              <TransferBadge transfer={player.latestTransfer}>{player.latestClubName}</TransferBadge>
            ) : (
              <span>{player.latestClubName}</span>
            )}
          </Box>
        }
        eyebrow="Player workspace"
        leading={
          <Box sx={{ flexShrink: 0 }}>
            <Box sx={{ display: { xs: 'none', sm: 'block' } }}>
              <IdentityMedia imageUrl={player.photoUrl} label={player.fullName} size={72} />
            </Box>
            <Box sx={{ display: { xs: 'block', sm: 'none' } }}>
              <IdentityMedia imageUrl={player.photoUrl} label={player.fullName} size={48} />
            </Box>
          </Box>
        }
        title={player.fullName}
      />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 0.9fr) minmax(0, 1.3fr)' },
        }}
      >
        <SurfaceCard>
          <Typography color="text.secondary" variant="caption">
            Profile
          </Typography>
          <Typography sx={{ mt: 1 }} variant="h3">
            {player.fullName}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
            Born {formatDateOfBirth(player.dateOfBirth, player.dateOfBirthPrecision)}
          </Typography>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: 2,
              mt: 3,
            }}
          >
            {stats.map((stat) => (
              <Stack key={stat.label} spacing={0.5}>
                <Typography color="text.secondary" variant="caption">
                  {stat.label}
                </Typography>
                <Typography variant="body1">{stat.value}</Typography>
              </Stack>
            ))}
          </Box>
        </SurfaceCard>

        <Stack spacing={2}>
          <SurfaceCard>
            <Typography color="primary.main" variant="caption">
              Latest club
            </Typography>
            <Typography sx={{ mt: 1 }} variant="h3">
              {player.latestClubName ?? 'Unattached / unknown'}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              {player.latestTransfer
                ? `Recent transfer · ${player.latestTransfer.seasonLabel ?? player.latestSeasonLabel ?? 'window'}`
                : player.latestSeasonLabel
                  ? `Most recent PlayerSeason · ${player.latestSeasonLabel}`
                  : 'No PlayerSeason history linked yet.'}
            </Typography>
            {player.latestClubId ? (
              <Button
                component={RouterLink}
                sx={{ mt: 2 }}
                to={routes.clubDetail(player.latestClubId)}
                variant="outlined"
              >
                Open club workspace
              </Button>
            ) : null}
          </SurfaceCard>
          <SurfaceCard accent="info">
            <Typography variant="h4">Build a scenario</Typography>
            <Typography color="text.secondary" sx={{ mt: 1, mb: 2 }} variant="body2">
              Project this profile at a destination club, then compare routes below.
            </Typography>
            <Button
              component={RouterLink}
              to={homePredictPath({ playerId: player.id })}
              variant="contained"
            >
              Build scenario
            </Button>
          </SurfaceCard>
        </Stack>
      </Box>

      <InjuryHistorySection playerId={player.id} />

      <ScenarioComparisonSection playerId={player.id} />
    </Stack>
  )
}
