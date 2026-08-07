import {
  Box,
  Link as MuiLink,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { HomeSection } from '@/components/home/HomeSection'
import { QueryState } from '@/components/common/QueryState'
import { homeSections, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import {
  fetchAllTimeStats,
  fetchTrendingStats,
  type LeaderboardEntry,
  type LeagueBoards,
} from '@/services/stats/statsApi'
import { formatNumber } from '@/utils/format'

function BoardTable({
  title,
  rows,
}: {
  title: string
  rows: LeaderboardEntry[]
}) {
  return (
    <Stack spacing={1}>
      <Typography component="h4" variant="h4">
        {title}
      </Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Player</TableCell>
            <TableCell align="right">G</TableCell>
            <TableCell align="right">A</TableCell>
            <TableCell align="right">Apps</TableCell>
            <TableCell align="right">Mins</TableCell>
            <TableCell align="right">Seasons</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow hover key={`${title}-${row.playerId}`}>
              <TableCell>
                <MuiLink component={RouterLink} to={routes.playerDetail(row.playerId)} underline="hover">
                  {row.playerName}
                </MuiLink>
                {row.clubName ? (
                  <Typography color="text.secondary" component="span" sx={{ display: 'block' }} variant="caption">
                    {row.clubName}
                  </Typography>
                ) : null}
              </TableCell>
              <TableCell align="right">{formatNumber(row.goals)}</TableCell>
              <TableCell align="right">{formatNumber(row.assists)}</TableCell>
              <TableCell align="right">{formatNumber(row.appearances)}</TableCell>
              <TableCell align="right">{formatNumber(row.minutesPlayed)}</TableCell>
              <TableCell align="right">{formatNumber(row.seasonsPlayed)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  )
}

function LeagueBoardsGrid({ boards }: { boards: LeagueBoards[] }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gap: 4,
        gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' },
      }}
    >
      {boards.map((board) => (
        <Stack key={board.league} spacing={3}>
          <Typography component="h3" variant="h3">
            {board.tournamentName}
            {board.seasonLabel ? (
              <Typography color="text.secondary" component="span" sx={{ ml: 1 }} variant="body2">
                · {board.seasonLabel}
              </Typography>
            ) : null}
          </Typography>
          <BoardTable rows={board.topScorers} title="Top scorers" />
          <BoardTable rows={board.topAssisters} title="Top assisters" />
        </Stack>
      ))}
    </Box>
  )
}

export function TrendingPlayersSection() {
  const query = useQuery({
    queryKey: queryKeys.stats.trending('', 3),
    queryFn: () => fetchTrendingStats(undefined, 3),
  })

  return (
    <HomeSection
      description="Top 3 scorers and assisters in the latest season for Premier League and La Liga."
      id={homeSections.trending}
      title="Trending players"
    >
      <QueryState
        emptyDescription="Ingest player-seasons to populate boards."
        emptyTitle="No trending stats"
        error={query.error}
        isEmpty={!query.data?.length}
        isError={query.isError}
        isLoading={query.isLoading}
        onRetry={() => void query.refetch()}
      >
        {query.data ? <LeagueBoardsGrid boards={query.data} /> : null}
      </QueryState>
    </HomeSection>
  )
}

export function AllTimeLeadersSection() {
  const query = useQuery({
    queryKey: queryKeys.stats.allTime(10),
    queryFn: () => fetchAllTimeStats(10),
  })

  return (
    <HomeSection
      description="Career totals within each league across all ingested seasons (not cross-league career totals)."
      id={homeSections.allTime}
      title="All-time leaders"
    >
      <QueryState
        emptyDescription="Ingest player-seasons to populate boards."
        emptyTitle="No all-time stats"
        error={query.error}
        isEmpty={!query.data?.length}
        isError={query.isError}
        isLoading={query.isLoading}
        onRetry={() => void query.refetch()}
      >
        {query.data ? <LeagueBoardsGrid boards={query.data} /> : null}
      </QueryState>
    </HomeSection>
  )
}
