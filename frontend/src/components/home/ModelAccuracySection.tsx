import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Popover,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useMemo, useState, type MouseEvent } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { ScrollableTable } from '@/components/common/ScrollableTable'
import { ErrorState } from '@/components/common/ErrorState'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import {
  fetchModelAccuracy,
  type AccuracySample,
  type LeagueAccuracy,
  type MetricBlock,
  type ModelAccuracy,
} from '@/services/stats/statsApi'
import { formatNumber } from '@/utils/format'

/** Top-five leagues only — order matches product USP. */
const LEAGUE_ORDER = ['ENG', 'ESP', 'GER', 'ITA', 'FRA'] as const

function InfoIcon() {
  return (
    <svg fill="none" height="18" viewBox="0 0 24 24" width="18" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.8" />
      <path d="M12 10.5v6" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
      <circle cx="12" cy="7.5" fill="currentColor" r="1.1" />
    </svg>
  )
}

function MaeInfoButton() {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null)
  const open = Boolean(anchorEl)

  return (
    <>
      <IconButton
        aria-label="What is MAE?"
        onClick={(event: MouseEvent<HTMLElement>) => setAnchorEl(event.currentTarget)}
        size="small"
        sx={{
          color: 'text.secondary',
          '&:hover': { color: 'primary.main' },
        }}
      >
        <InfoIcon />
      </IconButton>
      <Popover
        anchorEl={anchorEl}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        onClose={() => setAnchorEl(null)}
        open={open}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <Box sx={{ maxWidth: 360, p: 2 }}>
          <Typography sx={{ mb: 1 }} variant="subtitle2">
            What is MAE?
          </Typography>
          <Typography color="text.secondary" variant="body2">
            MAE (Mean Absolute Error) is the average size of the miss between our prediction and what
            actually happened — |actual − predicted|, averaged across the backtest sample. Lower MAE
            means the model was closer on average. We report it separately for minutes, goals, and
            assists.
          </Typography>
        </Box>
      </Popover>
    </>
  )
}

function seasonRangeLabel(seasons: string[] | undefined): string {
  if (!seasons?.length) {
    return 'past seasons'
  }
  const first = seasons[0].replace('/', '–')
  const last = seasons[seasons.length - 1].replace('/', '–')
  if (first === last) {
    return first
  }
  // e.g. 2017–18 … 2025–26 → "2017–2026" style short range for copy
  const startYear = seasons[0].split('/')[0]
  const endYear = seasons[seasons.length - 1].split('/')[1]
  if (startYear && endYear) {
    return `${startYear}–20${endYear}`
  }
  return `${first}–${last}`
}

function SamplesTable({ samples }: { samples: AccuracySample[] }) {
  if (samples.length === 0) {
    return (
      <Typography color="text.secondary" variant="body2">
        No sample transfers in this backtest slice yet.
      </Typography>
    )
  }

  return (
    <ScrollableTable minWidth={640} sx={{ maxHeight: '60vh' }}>
      <Table size="small" stickyHeader>
        <TableHead>
          <TableRow>
            <TableCell>Transfer</TableCell>
            <TableCell>Season</TableCell>
            <TableCell align="right">Predicted</TableCell>
            <TableCell align="right">Actual</TableCell>
            <TableCell align="right" />
          </TableRow>
        </TableHead>
        <TableBody>
          {samples.map((sample) => (
            <TableRow key={`${sample.playerId ?? sample.player}-${sample.season}-${sample.club}`}>
              <TableCell>
                <Typography sx={{ overflowWrap: 'anywhere' }} variant="body2">
                  {sample.player}
                  {sample.position ? ` (${sample.position})` : ''} → {sample.club}
                </Typography>
              </TableCell>
              <TableCell>{sample.season}</TableCell>
              <TableCell align="right">
                {formatNumber(sample.predictedMinutes, 0)} min ·{' '}
                {formatNumber(Number(sample.predictedGoals), 1)} G ·{' '}
                {formatNumber(Number(sample.predictedAssists), 1)} A
              </TableCell>
              <TableCell align="right">
                {formatNumber(sample.actualMinutes, 0)} min ·{' '}
                {formatNumber(Number(sample.actualGoals), 1)} G ·{' '}
                {formatNumber(Number(sample.actualAssists), 1)} A
              </TableCell>
              <TableCell align="right">
                {sample.predictionId ? (
                  <Button
                    component={RouterLink}
                    size="small"
                    to={routes.predictionDetail(sample.predictionId)}
                  >
                    View
                  </Button>
                ) : null}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </ScrollableTable>
  )
}

function OverallMetricBar({ metrics }: { metrics: MetricBlock }) {
  if (!metrics.n || !metrics.minutes || !metrics.goals || !metrics.assists) {
    return (
      <Typography color="text.secondary" variant="body2">
        No aggregate evaluations yet.
      </Typography>
    )
  }

  return (
    <Box
      sx={{
        alignItems: { xs: 'flex-start', md: 'center' },
        backgroundColor: 'primary.main',
        borderRadius: 2,
        color: 'primary.contrastText',
        display: 'flex',
        flexDirection: { xs: 'column', md: 'row' },
        gap: { xs: 2, md: 3 },
        justifyContent: 'space-between',
        px: { xs: 2.5, md: 3 },
        py: { xs: 2, md: 2.25 },
      }}
    >
      <Stack spacing={0.25} sx={{ minWidth: { md: 160 } }}>
        <Typography sx={{ color: 'inherit', opacity: 0.85 }} variant="caption">
          Overall metric
        </Typography>
        <Typography sx={{ color: 'inherit', fontWeight: 500 }} variant="body2">
          Aggregate performance
        </Typography>
      </Stack>

      <Box
        sx={{
          display: 'flex',
          flexWrap: 'wrap',
          gap: { xs: 2, md: 4 },
          flexGrow: 1,
          justifyContent: { md: 'center' },
        }}
      >
        <Typography sx={{ color: 'inherit', fontWeight: 500 }} variant="body2">
          Minutes MAE: {formatNumber(metrics.minutes.mae, 0)}
        </Typography>
        <Typography sx={{ color: 'inherit', fontWeight: 500 }} variant="body2">
          Goals MAE: {formatNumber(metrics.goals.mae, 1)}
        </Typography>
        <Typography sx={{ color: 'inherit', fontWeight: 500 }} variant="body2">
          Assists MAE: {formatNumber(metrics.assists.mae, 1)}
        </Typography>
      </Box>

      <Typography
        sx={{ color: 'inherit', fontStyle: 'italic', opacity: 0.85, whiteSpace: 'nowrap' }}
        variant="body2"
      >
        n = {formatNumber(metrics.n, 0)} observations
      </Typography>
    </Box>
  )
}

function LeagueBenchmarkCard({
  league,
  onShowDetails,
}: {
  league: LeagueAccuracy
  onShowDetails: () => void
}) {
  const metrics = league.metrics

  return (
    <SurfaceCard
      sx={{
        display: 'flex',
        flexDirection: 'column',
        gap: 1.5,
        height: '100%',
        p: 2,
        borderRadius: 2,
        boxShadow: 'none',
      }}
    >
      <Stack direction="row" sx={{ alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}>
        <Typography sx={{ fontWeight: 500 }} variant="subtitle1">
          {league.leagueName}
        </Typography>
        <Typography color="text.secondary" variant="secondary">
          n={metrics?.n ? formatNumber(metrics.n, 0) : '—'}
        </Typography>
      </Stack>

      {metrics?.minutes && metrics.goals && metrics.assists ? (
        <Stack spacing={1}>
          {(
            [
              ['Min MAE', formatNumber(metrics.minutes.mae, 0)],
              ['Goal MAE', formatNumber(metrics.goals.mae, 1)],
              ['Asst MAE', formatNumber(metrics.assists.mae, 1)],
            ] as const
          ).map(([label, value]) => (
            <Stack
              direction="row"
              key={label}
              sx={{ alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}
            >
              <Typography color="text.secondary" sx={{ fontWeight: 400 }} variant="secondary">
                {label}
              </Typography>
              <Typography
                color="text.primary"
                sx={{ fontWeight: 500, fontVariantNumeric: 'tabular-nums' }}
                variant="body1"
              >
                {value}
              </Typography>
            </Stack>
          ))}
        </Stack>
      ) : (
        <Typography color="text.secondary" variant="body2">
          No evaluations yet
        </Typography>
      )}

      <Button
        onClick={onShowDetails}
        size="small"
        sx={{
          alignSelf: 'flex-start',
          color: 'primary.main',
          fontWeight: 500,
          mt: 'auto',
          px: 0,
          minWidth: 0,
          '&:hover': { backgroundColor: 'transparent', textDecoration: 'underline' },
        }}
        variant="text"
      >
        View detailed analysis →
      </Button>
    </SurfaceCard>
  )
}

function LeagueDetailsDialog({
  league,
  open,
  onClose,
}: {
  league: LeagueAccuracy | null
  open: boolean
  onClose: () => void
}) {
  if (!league) {
    return null
  }

  const metrics = league.metrics

  return (
    <Dialog fullWidth maxWidth="lg" onClose={onClose} open={open}>
      <DialogTitle>{league.leagueName} — backtest details</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <SurfaceCard accent="info">
            <Typography color="primary.main" variant="caption">
              Accuracy
            </Typography>
            {metrics?.n && metrics.minutes && metrics.goals && metrics.assists ? (
              <Typography variant="body2">
                {league.leagueName}: minutes MAE {formatNumber(metrics.minutes.mae, 0)} · goals MAE{' '}
                {formatNumber(metrics.goals.mae, 1)} · assists MAE{' '}
                {formatNumber(metrics.assists.mae, 1)} (n={metrics.n})
              </Typography>
            ) : (
              <Typography color="text.secondary" variant="body2">
                No evaluations yet
              </Typography>
            )}
          </SurfaceCard>
          <Typography variant="subtitle2">
            Transfers in the backtest sample ({league.samples?.length ?? 0})
          </Typography>
          <Typography color="text.secondary" variant="body2">
            All club-changers from completed seasons in this league: our prediction vs the player’s
            actual destination season stats.
          </Typography>
          <SamplesTable samples={league.samples ?? []} />
        </Stack>
      </DialogContent>
    </Dialog>
  )
}

export function ModelAccuracySection() {
  const [selectedLeague, setSelectedLeague] = useState<LeagueAccuracy | null>(null)
  const query = useQuery({
    queryKey: queryKeys.stats.modelAccuracy(),
    queryFn: fetchModelAccuracy,
    staleTime: 60_000,
  })

  const leagues = useMemo(() => {
    const data = query.data
    if (!data) {
      return []
    }
    return LEAGUE_ORDER.map((code) => data.byLeague?.[code])
      .filter((row): row is LeagueAccuracy => Boolean(row && row.metrics?.n > 0))
  }, [query.data])

  if (query.isLoading) {
    return (
      <SurfaceCard>
        <Typography color="text.secondary" variant="body2">
          Loading model accuracy…
        </Typography>
      </SurfaceCard>
    )
  }

  if (query.isError) {
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />
  }

  const data = query.data as ModelAccuracy | undefined
  if (!data) {
    return null
  }

  return (
    <Stack spacing={2.5}>
      <Box>
        <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
          <Typography variant="h5">Model Benchmarking</Typography>
          <MaeInfoButton />
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 0.75, maxWidth: 820 }} variant="body2">
          Projected vs. actual MAE across the top five leagues, {seasonRangeLabel(data.seasons)}{' '}
          (n={formatNumber(data.metrics?.n ?? 0, 0)}).
        </Typography>
      </Box>

      <OverallMetricBar metrics={data.metrics} />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: {
            xs: '1fr',
            sm: 'repeat(2, minmax(0, 1fr))',
            lg: 'repeat(5, minmax(0, 1fr))',
          },
        }}
      >
        {leagues.map((league) => (
          <LeagueBenchmarkCard
            key={league.countryCode}
            league={league}
            onShowDetails={() => setSelectedLeague(league)}
          />
        ))}
      </Box>

      <LeagueDetailsDialog
        league={selectedLeague}
        onClose={() => setSelectedLeague(null)}
        open={selectedLeague != null}
      />
    </Stack>
  )
}
