import {
  Button,
  FormControl,
  InputLabel,
  Link as MuiLink,
  MenuItem,
  Pagination,
  Select,
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
import { useEffect, useMemo, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listPlayers, type PlayerListFilters } from '@/services/player/playerApi'
import { formatFootballCountry } from '@/utils/footballCountry'
import { formatAge } from '@/utils/format'
import { useDebouncedValue } from '@/utils/useDebouncedValue'

const PAGE_SIZE = 20

const POSITION_OPTIONS = [
  { value: '', label: 'Any position' },
  { value: 'GK', label: 'Goalkeeper' },
  { value: 'DEF', label: 'Defender' },
  { value: 'MID', label: 'Midfielder' },
  { value: 'FWD', label: 'Forward' },
] as const

const LEAGUE_OPTIONS = [
  { value: '', label: 'Any league' },
  { value: 'Premier League', label: 'Premier League' },
  { value: 'La Liga', label: 'La Liga' },
  { value: 'Bundesliga', label: 'Bundesliga' },
  { value: 'Serie A', label: 'Serie A' },
  { value: 'Ligue 1', label: 'Ligue 1' },
] as const

const AGE_BAND_OPTIONS = [
  { value: '', label: 'Any age', minAge: undefined, maxAge: undefined },
  { value: 'u21', label: 'Under 21', minAge: undefined, maxAge: 20 },
  { value: '21-25', label: '21–25', minAge: 21, maxAge: 25 },
  { value: '26-30', label: '26–30', minAge: 26, maxAge: 30 },
  { value: '31+', label: '31+', minAge: 31, maxAge: undefined },
] as const

export function PlayersPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [position, setPosition] = useState('')
  const [league, setLeague] = useState('')
  const [ageBand, setAgeBand] = useState('')
  const debounced = useDebouncedValue(search)

  const filters = useMemo<PlayerListFilters>(() => {
    const band = AGE_BAND_OPTIONS.find((option) => option.value === ageBand)
    return {
      query: debounced || undefined,
      position: position || undefined,
      league: league || undefined,
      minAge: band?.minAge,
      maxAge: band?.maxAge,
    }
  }, [ageBand, debounced, league, position])

  useEffect(() => {
    setPage(0)
  }, [filters.query, filters.position, filters.league, filters.minAge, filters.maxAge])

  const playersQuery = useQuery({
    queryKey: queryKeys.players.list(page, PAGE_SIZE, filters),
    queryFn: () => listPlayers(page, PAGE_SIZE, filters),
  })

  return (
    <Stack spacing={3}>
      <PageHeader
        description={`${playersQuery.data?.totalElements ?? '—'} profiles across monitored leagues.`}
        eyebrow="Global player index"
        title="Players Catalogue"
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
        <TextField
          fullWidth
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search player or nationality"
          value={search}
        />
        <FormControl sx={{ minWidth: { md: 160 } }}>
          <InputLabel id="player-position-filter">Position</InputLabel>
          <Select
            label="Position"
            labelId="player-position-filter"
            onChange={(event) => setPosition(String(event.target.value))}
            value={position}
          >
            {POSITION_OPTIONS.map((option) => (
              <MenuItem key={option.value || 'any'} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl sx={{ minWidth: { md: 180 } }}>
          <InputLabel id="player-league-filter">League</InputLabel>
          <Select
            label="League"
            labelId="player-league-filter"
            onChange={(event) => setLeague(String(event.target.value))}
            value={league}
          >
            {LEAGUE_OPTIONS.map((option) => (
              <MenuItem key={option.value || 'any'} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl sx={{ minWidth: { md: 140 } }}>
          <InputLabel id="player-age-filter">Age</InputLabel>
          <Select
            label="Age"
            labelId="player-age-filter"
            onChange={(event) => setAgeBand(String(event.target.value))}
            value={ageBand}
          >
            {AGE_BAND_OPTIONS.map((option) => (
              <MenuItem key={option.value || 'any'} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
        <QueryState
          emptyDescription="No players match these filters. Try widening position, league, or age."
          emptyTitle="No players found"
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
                    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                      <IdentityMedia
                        imageUrl={player.photoUrl}
                        label={player.fullName}
                        size={36}
                      />
                      <Stack spacing={0.25} sx={{ minWidth: 0 }}>
                        <MuiLink
                          component={RouterLink}
                          to={routes.playerDetail(player.id)}
                          underline="hover"
                        >
                          {player.fullName}
                        </MuiLink>
                        <Typography color="text.secondary" variant="caption">
                          {player.primaryPosition} · {formatFootballCountry(player.nationality)}
                        </Typography>
                      </Stack>
                    </Stack>
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
