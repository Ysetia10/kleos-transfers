import {
  Box,
  Button,
  Pagination,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listClubs } from '@/services/club/clubApi'
import { formatFootballCountry } from '@/utils/footballCountry'
import { useDebouncedValue } from '@/utils/useDebouncedValue'

const PAGE_SIZE = 12

export function ClubsPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const debounced = useDebouncedValue(search)

  useEffect(() => {
    setPage(0)
  }, [debounced])

  const clubsQuery = useQuery({
    queryKey: queryKeys.clubs.list(page, PAGE_SIZE, debounced),
    queryFn: () => listClubs(page, PAGE_SIZE, debounced || undefined),
  })

  return (
    <Stack spacing={3}>
      <PageHeader
        description={`${clubsQuery.data?.totalElements ?? '—'} clubs available as prediction destinations.`}
        eyebrow="Target club index"
        title="Clubs Catalogue"
      />

      <TextField
        fullWidth
        onChange={(event) => setSearch(event.target.value)}
        placeholder="Search club or country"
        value={search}
      />

      <QueryState
        emptyDescription="Import clubs through the API, then refresh."
        emptyTitle="No clubs yet"
        error={clubsQuery.error}
        isEmpty={!!clubsQuery.data?.empty}
        isError={clubsQuery.isError}
        isLoading={clubsQuery.isLoading}
        onRetry={() => void clubsQuery.refetch()}
      >
        <Box
          sx={{
            display: 'grid',
            gap: 2,
            gridTemplateColumns: {
              xs: '1fr',
              sm: 'repeat(2, minmax(0, 1fr))',
              lg: 'repeat(3, minmax(0, 1fr))',
            },
          }}
        >
          {clubsQuery.data?.content.map((club) => (
            <SurfaceCard key={club.id} sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                <IdentityMedia
                  imageUrl={club.crestUrl}
                  label={club.name}
                  rounded="soft"
                  size={44}
                />
                <Stack spacing={0.25} sx={{ minWidth: 0 }}>
                  <Typography noWrap variant="h4">
                    {club.name}
                  </Typography>
                  <Typography color="text.secondary" variant="body2">
                    {formatFootballCountry(club.countryCode)}
                  </Typography>
                </Stack>
              </Stack>

              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: 2,
                  borderTop: (theme) => `1px solid ${theme.palette.divider}`,
                  pt: 2,
                }}
              >
                <Stack spacing={0.5}>
                  <Typography color="text.secondary" variant="caption">
                    Manager
                  </Typography>
                  <Typography variant="body2">{club.currentManagerName ?? '—'}</Typography>
                </Stack>
                <Stack spacing={0.5}>
                  <Typography color="text.secondary" variant="caption">
                    Season
                  </Typography>
                  <Typography variant="body2">
                    {club.currentManagerSeasonLabel ?? '—'}
                  </Typography>
                </Stack>
              </Box>

              <Button
                component={RouterLink}
                fullWidth
                to={routes.clubDetail(club.id)}
                variant="outlined"
              >
                Open club workspace
              </Button>
            </SurfaceCard>
          ))}
        </Box>

        {(clubsQuery.data?.totalPages ?? 0) > 1 ? (
          <Stack
            direction="row"
            sx={{ alignItems: 'center', justifyContent: 'space-between', mt: 2 }}
          >
            <Typography color="text.secondary" variant="body2">
              Page {page + 1} of {clubsQuery.data?.totalPages}
            </Typography>
            <Pagination
              count={clubsQuery.data?.totalPages ?? 1}
              onChange={(_, value) => setPage(value - 1)}
              page={page + 1}
            />
          </Stack>
        ) : null}
      </QueryState>
    </Stack>
  )
}
