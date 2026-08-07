import {
  Box,
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
import { HomeSection } from '@/components/home/HomeSection'
import { QueryState } from '@/components/common/QueryState'
import { homeSections, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listClubs } from '@/services/club/clubApi'
import { listPlayers } from '@/services/player/playerApi'
import { formatDate } from '@/utils/format'

const PAGE_SIZE = 8

export function CatalogueSection() {
  const [playerPage, setPlayerPage] = useState(0)
  const [clubPage, setClubPage] = useState(0)

  const playersQuery = useQuery({
    queryKey: queryKeys.players.list(playerPage, PAGE_SIZE),
    queryFn: () => listPlayers(playerPage, PAGE_SIZE),
  })
  const clubsQuery = useQuery({
    queryKey: queryKeys.clubs.list(clubPage, PAGE_SIZE),
    queryFn: () => listClubs(clubPage, PAGE_SIZE),
  })

  return (
    <HomeSection
      description="Identity records used as prediction inputs. Open a row for details, or jump into a what-if from the detail page."
      id={homeSections.catalogue}
      title="Players & clubs"
    >
      <Box
        sx={{
          display: 'grid',
          gap: 4,
          gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' },
        }}
      >
        <Stack spacing={2}>
          <Typography component="h3" variant="h3">
            Players
          </Typography>
          <QueryState
            emptyDescription="Import players through the API, then refresh."
            emptyTitle="No players yet"
            error={playersQuery.error}
            isEmpty={!!playersQuery.data?.empty}
            isError={playersQuery.isError}
            isLoading={playersQuery.isLoading}
            onRetry={() => void playersQuery.refetch()}
          >
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Pos</TableCell>
                  <TableCell>Nat</TableCell>
                  <TableCell>Born</TableCell>
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
                    </TableCell>
                    <TableCell>{player.primaryPosition}</TableCell>
                    <TableCell>{player.nationality}</TableCell>
                    <TableCell>{formatDate(player.dateOfBirth)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
            {(playersQuery.data?.totalPages ?? 0) > 1 ? (
              <Stack direction="row" sx={{ justifyContent: 'space-between', mt: 2 }}>
                <Typography color="text.secondary" variant="body2">
                  {playersQuery.data?.totalElements} players
                </Typography>
                <Pagination
                  count={playersQuery.data?.totalPages ?? 1}
                  onChange={(_, value) => setPlayerPage(value - 1)}
                  page={playerPage + 1}
                  size="small"
                />
              </Stack>
            ) : null}
          </QueryState>
        </Stack>

        <Stack spacing={2}>
          <Typography component="h3" variant="h3">
            Clubs
          </Typography>
          <QueryState
            emptyDescription="Import clubs through the API, then refresh."
            emptyTitle="No clubs yet"
            error={clubsQuery.error}
            isEmpty={!!clubsQuery.data?.empty}
            isError={clubsQuery.isError}
            isLoading={clubsQuery.isLoading}
            onRetry={() => void clubsQuery.refetch()}
          >
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Name</TableCell>
                  <TableCell>Short</TableCell>
                  <TableCell>Country</TableCell>
                  <TableCell>Founded</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {clubsQuery.data?.content.map((club) => (
                  <TableRow hover key={club.id}>
                    <TableCell>
                      <MuiLink
                        component={RouterLink}
                        to={routes.clubDetail(club.id)}
                        underline="hover"
                      >
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
            {(clubsQuery.data?.totalPages ?? 0) > 1 ? (
              <Stack direction="row" sx={{ justifyContent: 'space-between', mt: 2 }}>
                <Typography color="text.secondary" variant="body2">
                  {clubsQuery.data?.totalElements} clubs
                </Typography>
                <Pagination
                  count={clubsQuery.data?.totalPages ?? 1}
                  onChange={(_, value) => setClubPage(value - 1)}
                  page={clubPage + 1}
                  size="small"
                />
              </Stack>
            ) : null}
          </QueryState>
        </Stack>
      </Box>
    </HomeSection>
  )
}
