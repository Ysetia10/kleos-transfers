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
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import {
  fetchAllTimeStats,
  fetchTrendingStats,
  type LeaderboardEntry,
  type LeagueBoards,
} from '@/services/stats/statsApi'
import { listTransfers } from '@/services/transfer/transferApi'
import { formatNumber } from '@/utils/format'

function BoardTable({ title, rows }: { title: string; rows: LeaderboardEntry[] }) {
  return (
    <Stack spacing={1.5}>
      <Typography variant="h4">{title}</Typography>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>Player</TableCell>
            <TableCell align="right">G</TableCell>
            <TableCell align="right">A</TableCell>
            <TableCell align="right">Mins</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row, index) => (
            <TableRow hover key={`${title}-${row.playerId ?? row.playerName}-${index}`}>
              <TableCell>
                {row.playerId ? (
                  <MuiLink
                    component={RouterLink}
                    to={routes.playerDetail(row.playerId)}
                    underline="hover"
                  >
                    {row.playerName}
                  </MuiLink>
                ) : (
                  row.playerName
                )}
                {row.clubName ? (
                  <Typography color="text.secondary" component="span" sx={{ display: 'block' }} variant="caption">
                    {row.clubName}
                  </Typography>
                ) : null}
              </TableCell>
              <TableCell align="right">{formatNumber(row.goals)}</TableCell>
              <TableCell align="right">{formatNumber(row.assists)}</TableCell>
              <TableCell align="right">
                {row.minutesPlayed > 0 ? formatNumber(row.minutesPlayed) : '—'}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Stack>
  )
}

function LeagueGrid({ boards }: { boards: LeagueBoards[] }) {
  return (
    <Box
      sx={{
        display: 'grid',
        gap: 2,
        gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' },
      }}
    >
      {boards.map((board) => (
        <SurfaceCard key={board.league}>
          <Stack spacing={3}>
            <Stack spacing={0.5}>
              <Typography color="primary.main" variant="caption">
                {board.seasonLabel ?? 'Career leaders'}
              </Typography>
              <Typography variant="h3">{board.tournamentName}</Typography>
              {board.coverageNote ? (
                <Typography color="text.secondary" variant="caption">
                  {board.coverageNote}
                </Typography>
              ) : null}
            </Stack>
            <BoardTable rows={board.topScorers} title="Top scorers" />
            <BoardTable rows={board.topAssisters} title="Top assisters" />
          </Stack>
        </SurfaceCard>
      ))}
    </Box>
  )
}

export function TrendingPage() {
  const trendingQuery = useQuery({
    queryKey: queryKeys.stats.trending('', 5),
    queryFn: () => fetchTrendingStats(undefined, 5),
  })
  const allTimeQuery = useQuery({
    queryKey: queryKeys.stats.allTime(8),
    queryFn: () => fetchAllTimeStats(8),
  })
  const transfersQuery = useQuery({
    queryKey: queryKeys.transfers.list(0, 8, 'COMPLETED'),
    queryFn: () => listTransfers(0, 8, 'COMPLETED'),
  })

  const highlight = trendingQuery.data?.[0]?.topScorers[0]

  return (
    <Stack spacing={4}>
      <PageHeader
        description="Output leaders from the latest ingested season, plus career totals within each league."
        eyebrow="Transfer intelligence"
        title="Trending"
      />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' },
        }}
      >
        <SurfaceCard accent="positive">
          <Typography color="text.secondary" variant="caption">
            Rising scorer
          </Typography>
          <Typography sx={{ mt: 1 }} variant="h2">
            {highlight ? formatNumber(highlight.goals) : '—'}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
            {highlight ? `${highlight.playerName} · goals` : 'Awaiting season boards'}
          </Typography>
        </SurfaceCard>
        <SurfaceCard accent="info">
          <Typography color="text.secondary" variant="caption">
            Coverage
          </Typography>
          <Typography sx={{ mt: 1 }} variant="h2">
            {trendingQuery.data?.length ?? '—'}
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
            {trendingQuery.data?.length
              ? trendingQuery.data.map((board) => board.tournamentName).join(' · ')
              : 'Top-five European leagues'}
          </Typography>
        </SurfaceCard>
        <SurfaceCard>
          <Typography color="text.secondary" variant="caption">
            Engine
          </Typography>
          <Typography sx={{ mt: 1 }} variant="h2">
            v0.3
          </Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
            Dimensional compatibility in production
          </Typography>
        </SurfaceCard>
      </Box>

      <Stack spacing={2}>
        <Typography variant="h3">Recent completed transfers</Typography>
        <Typography color="text.secondary" variant="body2">
          Inferred from consecutive club seasons (COMPLETED). Rumours use a separate status and are
          not mixed into this list.
        </Typography>
        <QueryState
          emptyDescription="Run scripts/infer_transfers_from_seasons.py after ingest."
          emptyTitle="No transfers yet"
          error={transfersQuery.error}
          isEmpty={!transfersQuery.data?.content.length}
          isError={transfersQuery.isError}
          isLoading={transfersQuery.isLoading}
          onRetry={() => void transfersQuery.refetch()}
        >
          <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Player</TableCell>
                  <TableCell>From</TableCell>
                  <TableCell>To</TableCell>
                  <TableCell>Season</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {transfersQuery.data?.content.map((transfer) => (
                  <TableRow hover key={transfer.id}>
                    <TableCell>
                      <MuiLink
                        component={RouterLink}
                        to={routes.playerDetail(transfer.playerId)}
                        underline="hover"
                      >
                        {transfer.playerName}
                      </MuiLink>
                    </TableCell>
                    <TableCell>{transfer.fromClubName ?? '—'}</TableCell>
                    <TableCell>{transfer.toClubName ?? '—'}</TableCell>
                    <TableCell>{transfer.seasonLabel}</TableCell>
                    <TableCell>{transfer.status}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </SurfaceCard>
        </QueryState>
      </Stack>

      <Stack spacing={2}>
        <Typography variant="h3">Latest season boards</Typography>
        <QueryState
          emptyDescription="Ingest player-seasons to populate boards."
          emptyTitle="No trending stats"
          error={trendingQuery.error}
          isEmpty={!trendingQuery.data?.length}
          isError={trendingQuery.isError}
          isLoading={trendingQuery.isLoading}
          onRetry={() => void trendingQuery.refetch()}
        >
          {trendingQuery.data ? <LeagueGrid boards={trendingQuery.data} /> : null}
        </QueryState>
      </Stack>

      <Stack spacing={2}>
        <Typography variant="h3">All-time leaders</Typography>
        <Typography color="text.secondary" variant="body2">
          Wikipedia-curated career leaders for PL and La Liga when available; otherwise totals
          within loaded FBref seasons (since 2016/17).
        </Typography>
        <QueryState
          emptyDescription="Ingest player-seasons to populate boards."
          emptyTitle="No all-time stats"
          error={allTimeQuery.error}
          isEmpty={!allTimeQuery.data?.length}
          isError={allTimeQuery.isError}
          isLoading={allTimeQuery.isLoading}
          onRetry={() => void allTimeQuery.refetch()}
        >
          {allTimeQuery.data ? <LeagueGrid boards={allTimeQuery.data} /> : null}
        </QueryState>
      </Stack>
    </Stack>
  )
}
