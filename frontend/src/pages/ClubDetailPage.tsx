import { Button, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink, useParams } from 'react-router-dom'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { PageHeader } from '@/components/common/PageHeader'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClub } from '@/services/club/clubApi'

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
  const fields = [
    ['Name', club.name],
    ['Short name', club.shortName],
    ['Country', club.countryCode],
    ['Founded', club.foundedYear?.toString() ?? '—'],
    ['FBref id', club.fbrefId ?? '—'],
  ] as const

  return (
    <Stack spacing={3}>
      <PageHeader
        actions={
          <Button
            component={RouterLink}
            to={`${routes.prediction}?clubId=${club.id}`}
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
