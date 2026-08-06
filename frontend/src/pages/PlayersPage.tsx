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
import { listPlayers } from '@/services/player/playerApi'
import { formatDate } from '@/utils/format'

const PAGE_SIZE = 20

export function PlayersPage() {
  const [page, setPage] = useState(0)
  const query = useQuery({
    queryKey: queryKeys.players.list(page, PAGE_SIZE),
    queryFn: () => listPlayers(page, PAGE_SIZE),
  })

  return (
    <Stack spacing={0}>
      <PageHeader
        description="Identity records used as inputs for transfer what-if predictions."
        title="Players"
      />
      <QueryState
        emptyDescription="Create players through the API or bulk import, then refresh."
        emptyTitle="No players yet"
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
              <TableCell>Position</TableCell>
              <TableCell>Nationality</TableCell>
              <TableCell>Date of birth</TableCell>
              <TableCell>Foot</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {query.data?.content.map((player) => (
              <TableRow hover key={player.id}>
                <TableCell>
                  <MuiLink
                    component={RouterLink}
                    to={routes.playerDetail(player.id)}
                    underline="hover"
                  >
                    {player.fullName}
                  </MuiLink>
                </TableCell>
                <TableCell>{player.primaryPosition}</TableCell>
                <TableCell>{player.nationality}</TableCell>
                <TableCell>{formatDate(player.dateOfBirth)}</TableCell>
                <TableCell>{player.preferredFoot}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        {(query.data?.totalPages ?? 0) > 1 ? (
          <Stack direction="row" sx={{ justifyContent: 'space-between', mt: 3 }}>
            <Typography color="text.secondary" variant="body2">
              {query.data?.totalElements} players
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
