import {
  Link as MuiLink,
  Pagination,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listClubs } from '@/services/club/clubApi'

const PAGE_SIZE = 20

export function ClubsPage() {
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: queryKeys.clubs.list(page, PAGE_SIZE),
    queryFn: () => listClubs(page, PAGE_SIZE),
  })

  return (
    <Stack spacing={0}>
      <PageHeader
        description="Club identities used as transfer destinations and historical context."
        title="Clubs"
      />
      <QueryState
        emptyDescription="Create clubs through the API or bulk import, then refresh."
        emptyTitle="No clubs yet"
        error={query.error}
        isEmpty={!!query.data?.empty}
        isError={query.isError}
        isLoading={query.isLoading}
        onRetry={() => void query.refetch()}
      >
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Name</TableCell>
              <TableCell>Short name</TableCell>
              <TableCell>Country</TableCell>
              <TableCell>Founded</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {query.data?.content.map((club) => (
              <TableRow hover key={club.id}>
                <TableCell>
                  <MuiLink component={RouterLink} to={routes.clubDetail(club.id)} underline="hover">
                    {club.name}
                  </MuiLink>
                </TableCell>
                <TableCell>{club.shortName}</TableCell>
                <TableCell>{club.countryCode}</TableCell>
                <TableCell>{club.foundedYear ?? '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {(query.data?.totalPages ?? 0) > 1 ? (
          <Stack direction="row" sx={{ justifyContent: 'space-between', mt: 3 }}>
            <Typography color="text.secondary" variant="body2">
              {query.data?.totalElements} clubs
            </Typography>
            <Pagination
              count={query.data?.totalPages ?? 1}
              onChange={(_, value) => setPage(value - 1)}
              page={page + 1}
            />
          </Stack>
        ) : null}
      </QueryState>
    </Stack>
  )
}
