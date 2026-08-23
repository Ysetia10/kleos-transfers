import { Box, Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { LoadingState } from '@/components/common/LoadingState'
import { NewBadge } from '@/components/common/NewBadge'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { SquadTable } from '@/components/home/SquadTable'
import { PitchLineup } from '@/components/squad/PitchLineup'
import { homePredictPath } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClub } from '@/services/club/clubApi'
import { getClubSquad } from '@/services/club/squadApi'
import { listSeasons } from '@/services/season/seasonApi'
import type { Season } from '@/types/domain'
import { formatFootballCountry } from '@/utils/footballCountry'

function isUpcomingSeason(season: Season, today = new Date()): boolean {
  return new Date(`${season.endDate}T23:59:59`) >= today
}

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
  // Prefer the upcoming campaign so squad depth applies prior roster ± window transfers.
  const squadSeason = useMemo(() => {
    if (!seasons.length) {
      return null
    }
    const upcoming = seasons.find((season) => isUpcomingSeason(season))
    if (upcoming) {
      return upcoming
    }
    return (
      seasons.find((season) => season.label === club?.currentManagerSeasonLabel) ?? seasons[0]
    )
  }, [seasons, club?.currentManagerSeasonLabel])
  const seasonId = squadSeason?.id
  const priorSeason = useMemo(() => {
    if (!squadSeason) {
      return null
    }
    return (
      seasons
        .filter((season) => season.startDate < squadSeason.startDate)
        .sort((a, b) => b.startDate.localeCompare(a.startDate))[0] ?? null
    )
  }, [seasons, squadSeason])

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

  const seasonLabel = squadSeason?.label ?? club.currentManagerSeasonLabel
  const projectedSquad =
    !!squadSeason && isUpcomingSeason(squadSeason) && !!priorSeason && !!squadQuery.data?.length

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
          <IdentityMedia imageUrl={club.crestUrl} label={club.name} rounded="soft" size={72} />
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
              Fit index
            </Typography>
            <Typography sx={{ mt: 1 }} variant="h3">
              {Number.isFinite(club.fitIndex) ? Math.round(club.fitIndex) : '—'}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              Club environment score from system, tempo, and youth minutes. Recruitment signal:{' '}
              {club.recruitmentSignal}.
            </Typography>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 1.5,
                mt: 2,
              }}
            >
              <Stack spacing={0.25}>
                <Typography color="text.secondary" variant="caption">
                  System
                </Typography>
                <Typography variant="body2">{club.tacticalSystem ?? '—'}</Typography>
              </Stack>
              <Stack spacing={0.25}>
                <Typography color="text.secondary" variant="caption">
                  Tempo
                </Typography>
                <Typography variant="body2">{club.tempo ?? '—'}</Typography>
              </Stack>
              <Stack spacing={0.25}>
                <Typography color="text.secondary" variant="caption">
                  Youth minutes
                </Typography>
                <Typography variant="body2">
                  {club.youthMinutesPct == null
                    ? '—'
                    : `${Number(club.youthMinutesPct).toFixed(1)}%`}
                </Typography>
              </Stack>
              <Stack spacing={0.25}>
                <Typography color="text.secondary" variant="caption">
                  Season
                </Typography>
                <Typography variant="body2">
                  {club.currentManagerSeasonLabel ?? '—'}
                </Typography>
              </Stack>
            </Box>
          </SurfaceCard>
          <SurfaceCard>
            <Typography color="primary.main" variant="caption">
              Manager
            </Typography>
            <Typography sx={{ mt: 1 }} variant="h3">
              {club.currentManagerName ? (
                <>
                  {club.currentManagerName}
                  {club.currentManagerFirstSeasonAtClub ? <NewBadge /> : null}
                </>
              ) : (
                'No manager on file'
              )}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              {club.currentManagerName
                ? club.currentManagerFirstSeasonAtClub
                  ? `First season at ${club.name}${
                      club.currentManagerSeasonLabel ? ` · ${club.currentManagerSeasonLabel}` : ''
                    }.`
                  : `At ${club.name} since ${
                      club.currentManagerSinceSeasonLabel ?? club.currentManagerSeasonLabel ?? '—'
                    }.`
                : 'Import manager appointments to populate this club’s coaching context.'}
            </Typography>
          </SurfaceCard>
          <SurfaceCard>
            <Typography variant="h4">Test a player here</Typography>
            <Typography color="text.secondary" sx={{ mt: 1, mb: 2 }} variant="body2">
              Project how a player would fare in this squad and league context.
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
          {projectedSquad ? (
            <Typography color="text.secondary" variant="body2">
              Working squad from {priorSeason.label} ± confirmed/announced {seasonLabel} transfers.
              Minutes still reflect the prior campaign.
            </Typography>
          ) : null}
          {!squadQuery.isLoading && !squadQuery.isError ? (
            <PitchLineup clubId={id} seasonId={seasonId} squad={squadQuery.data} />
          ) : null}
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
