import {
  Button,
  Link as MuiLink,
  Pagination,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listPlayers } from '@/services/player/playerApi'
import { formatFootballCountry } from '@/utils/footballCountry'
import { formatAge } from '@/utils/format'

const PAGE_SIZE = 20

export function PlayersPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')

  useEffect(() => {
    const handle = window.setTimeout(() => setDebounced(search), 250)
    return () => window.clearTimeout(handle)
  }, [search])

  useEffect(() => {
    setPage(0)
  }, [debounced])

  const playersQuery = useQuery({
    queryKey: queryKeys.players.list(page, PAGE_SIZE, debounced),
    queryFn: () => listPlayers(page, PAGE_SIZE, debounced || undefined),
  })

  return (
    <Stack spacing={3}>
      <PageHeader
        description={`${playersQuery.data?.totalElements ?? '—'} profiles across monitored leagues.`}
        eyebrow="Global player index"
        title="Players Catalogue"
      />

      <TextField
        fullWidth
        onChange={(event) => setSearch(event.target.value)}
        placeholder="Search player or nationality"
        value={search}
      />

      <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
        <QueryState
          emptyDescription="Import players through the API, then refresh."
          emptyTitle="No players yet"
          error={playersQuery.error}
          isEmpty={!!playersQuery.data?.empty}
          isError={playersQuery.isError}
          isLoading={playersQuery.isLoading}
          onRetry={() => void playersQuery.refetch()}
        >
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Player</TableCell>
                <TableCell>Current club</TableCell>
                <TableCell>Age</TableCell>
                <TableCell>Nationality</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {playersQuery.data?.content.map((player) => (
                <TableRow hover key={player.id}>
                  <TableCell>
                    <MuiLink
                      component={RouterLink}
                      to={routes.playerDetail(player.id)}
                      underline="hover"
                    >
                      {player.fullName}
                    </MuiLink>
                    <Typography color="text.secondary" sx={{ display: 'block' }} variant="caption">
                      {player.primaryPosition} · {formatFootballCountry(player.nationality)}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {player.latestClubId && player.latestClubName ? (
                      <MuiLink
                        component={RouterLink}
                        to={routes.clubDetail(player.latestClubId)}
                        underline="hover"
                      >
                        {player.latestClubName}
                      </MuiLink>
                    ) : (
                      '—'
                    )}
                  </TableCell>
                  <TableCell>{formatAge(player.age)}</TableCell>
                  <TableCell>{formatFootballCountry(player.nationality)}</TableCell>
                  <TableCell align="right">
                    <Button
                      component={RouterLink}
                      size="small"
                      to={homePredictPath({ playerId: player.id })}
                      variant="outlined"
                    >
                      Predict transfer
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {(playersQuery.data?.totalPages ?? 0) > 1 ? (
            <Stack
              direction="row"
              sx={{ alignItems: 'center', justifyContent: 'space-between', px: 2, py: 2 }}
            >
              <Typography color="text.secondary" variant="body2">
                Showing page {page + 1} of {playersQuery.data?.totalPages}
              </Typography>
              <Pagination
                count={playersQuery.data?.totalPages ?? 1}
                onChange={(_, value) => setPage(value - 1)}
                page={page + 1}
              />
            </Stack>
          ) : null}
        </QueryState>
      </SurfaceCard>
    </Stack>
  )
}
