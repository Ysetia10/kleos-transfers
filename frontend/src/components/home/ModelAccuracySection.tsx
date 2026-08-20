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

function MetricLine({ label, block }: { label: string; block: MetricBlock | null | undefined }) {
  if (!block?.n || !block.minutes || !block.goals || !block.assists) {
    return (
      <Typography color="text.secondary" variant="body2">
        {label}: no evaluations yet
      </Typography>
    )
  }
  return (
    <Typography variant="body2">
      {label}: minutes MAE {formatNumber(block.minutes.mae, 0)} · goals MAE{' '}
      {formatNumber(block.goals.mae, 1)} · assists MAE {formatNumber(block.assists.mae, 1)} (n=
      {block.n})
    </Typography>
  )
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
    <Box sx={{ maxHeight: '60vh', overflow: 'auto' }}>
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
                <Typography variant="body2">
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
    </Box>
  )
}

function LeagueCard({
  league,
  onShowDetails,
}: {
  league: LeagueAccuracy
  onShowDetails: () => void
}) {
  return (
    <SurfaceCard>
      <Stack spacing={1.5}>
        <Typography color="primary.main" variant="caption">
          {league.leagueName}
        </Typography>
        <MetricLine label="Accuracy" block={league.metrics} />
        <Button onClick={onShowDetails} size="small" sx={{ alignSelf: 'flex-start' }} variant="outlined">
          Show all details
        </Button>
      </Stack>
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

  return (
    <Dialog fullWidth maxWidth="lg" onClose={onClose} open={open}>
      <DialogTitle>{league.leagueName} — backtest details</DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ pt: 1 }}>
          <SurfaceCard accent="info">
            <Typography color="primary.main" variant="caption">
              Accuracy
            </Typography>
            <MetricLine label={league.leagueName} block={league.metrics} />
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
    const order = ['ENG', 'ESP', 'GER', 'ITA', 'FRA']
    return Object.values(data.byLeague ?? {})
      .filter((row) => row.metrics?.n > 0)
      .sort((a, b) => order.indexOf(a.countryCode) - order.indexOf(b.countryCode))
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
    <Stack spacing={2}>
      <Box>
        <Stack alignItems="center" direction="row" spacing={0.5}>
          <Typography variant="h5">Model accuracy by league</Typography>
          <MaeInfoButton />
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
          How the model performed on real club-changers in {data.seasons?.join(', ') || 'past seasons'} —
          predicted next-club minutes, goals, and assists vs actual outcomes (n={data.metrics?.n ?? 0}).
        </Typography>
      </Box>

      <SurfaceCard accent="info">
        <Typography color="primary.main" variant="caption">
          Overall
        </Typography>
        <MetricLine label="All leagues" block={data.metrics} />
      </SurfaceCard>

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: {
            xs: '1fr',
            md: 'repeat(2, minmax(0, 1fr))',
            lg: 'repeat(3, minmax(0, 1fr))',
          },
        }}
      >
        {leagues.map((league) => (
          <LeagueCard
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
