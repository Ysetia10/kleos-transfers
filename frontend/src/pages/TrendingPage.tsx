import {
  Box,
  Button,
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
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import {
  fetchAllTimeStats,
  fetchHighestFitRoutes,
  fetchTrendingStats,
  type LeaderboardEntry,
  type LeagueBoards,
} from '@/services/stats/statsApi'
import { listSeasons } from '@/services/season/seasonApi'
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
            <BoardTable
              rows={board.topScorers}
              title={
                board.seasonLabel
                  ? `Previous season top scorers (${board.seasonLabel})`
                  : 'Previous season top scorers'
              }
            />
            <BoardTable
              rows={board.topAssisters}
              title={
                board.seasonLabel
                  ? `Previous season top assisters (${board.seasonLabel})`
                  : 'Previous season top assisters'
              }
            />
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
  const seasonsQuery = useQuery({
    queryKey: queryKeys.seasons.list(0, 20),
    queryFn: () => listSeasons(0, 20),
  })
  const latestWindowSeasonId =
    seasonsQuery.data?.content.find((season) => {
      const end = new Date(`${season.endDate}T23:59:59`)
      return end >= new Date()
    })?.id ?? seasonsQuery.data?.content[0]?.id

  const transfersQuery = useQuery({
    queryKey: queryKeys.transfers.list(0, 8, 'COMPLETED', latestWindowSeasonId ?? ''),
    queryFn: () => listTransfers(0, 8, 'COMPLETED', latestWindowSeasonId),
    enabled: !!latestWindowSeasonId,
  })
  const fitRoutesQuery = useQuery({
    queryKey: queryKeys.stats.fitRoutes(8),
    queryFn: () => fetchHighestFitRoutes(8),
  })

  const highlight = trendingQuery.data?.[0]?.topScorers[0]

  return (
    <Stack spacing={4}>
      <PageHeader
        description="Previous-season scoring boards (latest campaign with data — currently 2025/26 while 2026/27 is still empty), plus career totals and completed moves to project."
        eyebrow="Transfer intelligence"
        title="Trending"
      />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
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
      </Box>

      <Stack spacing={2}>
        <Typography variant="h3">Highest-fit transfer routes</Typography>
        <Typography color="text.secondary" variant="body2">
          Player → club routes ranked by compatibility. Uses recent simulator runs when available,
          otherwise a bounded batch of hypothetical top-scorer destinations.
        </Typography>
        <QueryState
          emptyDescription="Run a few predictions or wait for hypothetical route generation."
          emptyTitle="No fit routes yet"
          error={fitRoutesQuery.error}
          isEmpty={!fitRoutesQuery.data?.length}
          isError={fitRoutesQuery.isError}
          isLoading={fitRoutesQuery.isLoading}
          onRetry={() => void fitRoutesQuery.refetch()}
        >
          <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Route</TableCell>
                  <TableCell align="right">Fit</TableCell>
                  <TableCell align="right">xM</TableCell>
                  <TableCell align="right">Open</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {fitRoutesQuery.data?.map((route) => (
                  <TableRow hover key={`${route.playerId}-${route.toClubId}-${route.source}`}>
                    <TableCell>
                      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                        <IdentityMedia
                          imageUrl={route.playerPhotoUrl}
                          label={route.playerName}
                          size={36}
                        />
                        <Stack spacing={0.25} sx={{ minWidth: 0 }}>
                          <MuiLink
                            component={RouterLink}
                            to={routes.playerDetail(route.playerId)}
                            underline="hover"
                          >
                            {route.playerName}
                          </MuiLink>
                          <Typography color="text.secondary" variant="caption">
                            {(route.fromClubName ?? 'Free / unknown') + ' → '}
                            <MuiLink
                              component={RouterLink}
                              to={routes.clubDetail(route.toClubId)}
                              underline="hover"
                            >
                              {route.toClubName}
                            </MuiLink>
                            {' · '}
                            {route.seasonLabel}
                          </Typography>
                        </Stack>
                      </Stack>
                    </TableCell>
                    <TableCell align="right">
                      {formatNumber(Number(route.compatibilityScore), 0)}
                    </TableCell>
                    <TableCell align="right">{formatNumber(route.predictedMinutes)}</TableCell>
                    <TableCell align="right">
                      <Button
                        component={RouterLink}
                        size="small"
                        to={
                          route.predictionId
                            ? routes.predictionDetail(route.predictionId)
                            : homePredictPath({
                                playerId: route.playerId,
                                clubId: route.toClubId,
                              })
                        }
                        variant="outlined"
                      >
                        {route.predictionId ? 'View' : 'Simulate'}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </SurfaceCard>
        </QueryState>
      </Stack>

      <Stack spacing={2}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ alignItems: { sm: 'baseline' }, justifyContent: 'space-between' }}
        >
          <Stack spacing={0.5}>
            <Typography variant="h3">Recent completed transfers</Typography>
            <Typography color="text.secondary" variant="body2">
              Latest window deals (Wikipedia-dated) for the upcoming season. Open Transfers to
              project each signing at their new club.
            </Typography>
          </Stack>
          <Button component={RouterLink} to={routes.transfers} variant="outlined">
            View all transfers
          </Button>
        </Stack>
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
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </SurfaceCard>
        </QueryState>
      </Stack>

      <Stack spacing={2}>
        <Typography variant="h3">Previous season boards</Typography>
        <Typography color="text.secondary" variant="body2">
          Leaders from the latest campaign that has PlayerSeason data. Upcoming shells (e.g. 2026/27)
          stay out of these tables until outcomes exist.
        </Typography>
        <QueryState
          emptyDescription="Ingest player-seasons to populate boards."
          emptyTitle="No previous-season stats"
          error={trendingQuery.error}
          isEmpty={
            !trendingQuery.data?.some(
              (board) => board.topScorers.length > 0 || board.topAssisters.length > 0,
            )
          }
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
