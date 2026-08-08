import { Box, Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { IdentityMediaWithCredit } from '@/components/common/IdentityMedia'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { SquadTable } from '@/components/home/SquadTable'
import { homePredictPath } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClub } from '@/services/club/clubApi'
import { getClubSquad } from '@/services/club/squadApi'
import { listSeasons } from '@/services/season/seasonApi'
import { formatFootballCountry } from '@/utils/footballCountry'

export function ClubDetailPage() {
  const { id = '' } = useParams()
  const clubQuery = useQuery({
    queryKey: queryKeys.clubs.detail(id),
    queryFn: () => getClub(id),
    enabled: !!id,
  })

  const seasonsQuery = useQuery({
    queryKey: queryKeys.seasons.list(0, 50),
    queryFn: () => listSeasons(0, 50),
  })

  const club = clubQuery.data
  const seasons = seasonsQuery.data?.content ?? []
  const seasonId =
    seasons.find((season) => season.label === club?.currentManagerSeasonLabel)?.id ??
    seasons[0]?.id

  const squadQuery = useQuery({
    queryKey: queryKeys.clubs.squad(id, seasonId ?? ''),
    queryFn: () => getClubSquad(id, seasonId!),
    enabled: !!id && !!seasonId,
  })

  if (clubQuery.isLoading) {
    return <LoadingState />
  }
  if (clubQuery.isError || !club) {
    return <ErrorState error={clubQuery.error} onRetry={() => void clubQuery.refetch()} />
  }

  const seasonLabel =
    seasons.find((season) => season.id === seasonId)?.label ?? club.currentManagerSeasonLabel

  return (
    <Stack spacing={3}>
      <PageHeader
        actions={
          <Button
            component={RouterLink}
            to={homePredictPath({ clubId: club.id })}
            variant="contained"
          >
            Start prediction
          </Button>
        }
        description={`${formatFootballCountry(club.countryCode)}${
          club.foundedYear != null ? ` · Founded ${club.foundedYear}` : ''
        }`}
        eyebrow="Target club workspace"
        leading={
          <IdentityMediaWithCredit
            attribution={club.crestAttribution}
            imageUrl={club.crestUrl}
            label={club.name}
            license={club.crestLicense}
            rounded="soft"
            size={72}
          />
        }
        title={club.name}
      />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 0.9fr) minmax(0, 1.4fr)' },
        }}
      >
        <Stack spacing={2}>
          <SurfaceCard accent="info">
            <Typography color="primary.main" variant="caption">
              Manager context
            </Typography>
            <Typography sx={{ mt: 1 }} variant="h3">
              {club.currentManagerName ?? 'Manager unavailable'}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              {club.currentManagerSeasonLabel
                ? `Appointment linked for ${club.currentManagerSeasonLabel}.`
                : 'No ManagerSeason row for this club yet — import appointments to enrich fit signals.'}
            </Typography>
          </SurfaceCard>
          <SurfaceCard>
            <Typography variant="h4">Test a player in this system</Typography>
            <Typography color="text.secondary" sx={{ mt: 1, mb: 2 }} variant="body2">
              Run a what-if arrival with squad competition and league context from the simulator.
            </Typography>
            <Button
              component={RouterLink}
              to={homePredictPath({ clubId: club.id })}
              variant="contained"
            >
              Select player
            </Button>
          </SurfaceCard>
        </Stack>

        <Stack spacing={2}>
          <Typography variant="h3">
            Squad depth{seasonLabel ? ` · ${seasonLabel}` : ''}
          </Typography>
          <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
            <SquadTable
              error={squadQuery.error}
              isError={squadQuery.isError}
              isLoading={squadQuery.isLoading || seasonsQuery.isLoading}
              onRetry={() => void squadQuery.refetch()}
              squad={squadQuery.data}
            />
          </SurfaceCard>
        </Stack>
      </Box>
    </Stack>
  )
}
