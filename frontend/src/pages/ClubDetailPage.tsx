import { Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { homePredictPath } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClub } from '@/services/club/clubApi'
import { formatFootballCountry } from '@/utils/footballCountry'

export function ClubDetailPage() {
  const { id = '' } = useParams()
  const query = useQuery({
    queryKey: queryKeys.clubs.detail(id),
    queryFn: () => getClub(id),
    enabled: !!id,
  })

  if (query.isLoading) {
    return <LoadingState />
  }
  if (query.isError || !query.data) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const club = query.data
  const fields: Array<[string, ReactNode]> = [
    ['Name', club.name],
    ['Country', formatFootballCountry(club.countryCode)],
  ]
  if (club.currentManagerName) {
    fields.push([
      'Manager',
      club.currentManagerSeasonLabel
        ? `${club.currentManagerName} (${club.currentManagerSeasonLabel})`
        : club.currentManagerName,
    ])
  }
  if (club.foundedYear != null) {
    fields.push(['Founded', String(club.foundedYear)])
  }
  if (club.fbrefId) {
    fields.push(['FBref id', club.fbrefId])
  }

  return (
    <Stack spacing={3}>
      <PageHeader
        actions={
          <Button
            component={RouterLink}
            to={homePredictPath({ clubId: club.id })}
            variant="contained"
          >
            Predict arrival here
          </Button>
        }
        description="Permanent club identity — league membership lives on ClubSeason."
        title={club.name}
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
