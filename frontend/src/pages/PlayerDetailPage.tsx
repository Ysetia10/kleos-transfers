import { Button, Link as MuiLink, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getPlayer } from '@/services/player/playerApi'
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
  const latestClub =
    player.latestClubId && player.latestClubName ? (
      <MuiLink component={RouterLink} to={routes.clubDetail(player.latestClubId)} underline="hover">
        {player.latestClubName}
        {player.latestSeasonLabel ? ` · ${player.latestSeasonLabel}` : ''}
      </MuiLink>
    ) : (
      '—'
    )

  const fields: ReadonlyArray<readonly [string, ReactNode]> = [
    ['Full name', player.fullName],
    ['Age', formatAge(player.age)],
    ['Date of birth', formatDate(player.dateOfBirth)],
    ['Latest club', latestClub],
    ['Nationality', player.nationality],
    ['Height', player.heightCm == null ? '—' : `${player.heightCm} cm`],
    ['Preferred foot', player.preferredFoot ?? '—'],
    ['Primary position', player.primaryPosition],
    ['FBref id', player.fbrefId ?? '—'],
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
            Predict transfer
          </Button>
        }
        description="Identity attributes plus latest club from PlayerSeason history."
        title={player.fullName}
      />
      <Stack
        component="dl"
        spacing={2}
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { sm: 'repeat(2, minmax(0, 1fr))' },
          m: 0,
          maxWidth: 720,
        }}
      >
        {fields.map(([label, value]) => (
          <Stack component="div" key={label} spacing={0.5}>
            <Typography color="text.secondary" component="dt" variant="caption">
              {label}
            </Typography>
            <Typography component="dd" sx={{ m: 0 }} variant="body1">
              {value}
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  )
}
