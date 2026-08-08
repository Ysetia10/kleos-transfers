import { Box, Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { IdentityMediaWithCredit } from '@/components/common/IdentityMedia'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getPlayer } from '@/services/player/playerApi'
import { formatFootballCountry } from '@/utils/footballCountry'
import { formatAge, formatDate } from '@/utils/format'

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
        description={`${player.primaryPosition} · ${formatFootballCountry(player.nationality)}${
          player.latestClubName ? ` · ${player.latestClubName}` : ''
        }`}
        eyebrow="Player workspace"
        leading={
          <IdentityMediaWithCredit
            attribution={player.photoAttribution}
            imageUrl={player.photoUrl}
            label={player.fullName}
            license={player.photoLicense}
            size={72}
          />
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
            Born {formatDate(player.dateOfBirth)}
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
              {player.latestSeasonLabel
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
              Compare how this profile projects at a destination club for a chosen season.
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
    </Stack>
  )
}
