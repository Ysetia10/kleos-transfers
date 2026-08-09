import {
  Alert,
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
  Typography,
} from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Link as RouterLink, useNavigate } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { createPrediction } from '@/services/prediction/predictionApi'
import { listSeasons } from '@/services/season/seasonApi'
import {
  listTransfers,
  type Transfer,
  type TransferStatus,
} from '@/services/transfer/transferApi'
import { formatDate, formatEur } from '@/utils/format'
import { userFacingErrorMessage } from '@/utils/userFacingError'

const PAGE_SIZE = 20

const STATUS_OPTIONS: ReadonlyArray<{ value: '' | TransferStatus; label: string }> = [
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'ANNOUNCED', label: 'Announced' },
  { value: 'RUMOURED', label: 'Rumoured' },
  { value: '', label: 'All statuses' },
]

function isUpcomingSeason(endDate: string, today = new Date()): boolean {
  return new Date(`${endDate}T23:59:59`) >= today
}

export function TransfersPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<'' | TransferStatus>('COMPLETED')
  const [seasonId, setSeasonId] = useState('')
  const [pendingId, setPendingId] = useState<string | null>(null)

  const seasonsQuery = useQuery({
    queryKey: queryKeys.seasons.list(0, 50),
    queryFn: () => listSeasons(0, 50),
  })

  const seasons = seasonsQuery.data?.content ?? []
  const projectSeason = useMemo(() => {
    return seasons.find((season) => isUpcomingSeason(season.endDate)) ?? seasons[0] ?? null
  }, [seasons])

  // Default list filter to the upcoming / newest season (latest window), not old inferred years.
  useEffect(() => {
    if (!seasonId && projectSeason?.id) {
      setSeasonId(projectSeason.id)
    }
  }, [projectSeason?.id, seasonId])

  useEffect(() => {
    setPage(0)
  }, [status, seasonId])

  const transfersQuery = useQuery({
    queryKey: queryKeys.transfers.list(page, PAGE_SIZE, status, seasonId),
    queryFn: () =>
      listTransfers(page, PAGE_SIZE, status || undefined, seasonId || undefined),
    enabled: !!seasonId || seasonsQuery.isSuccess,
  })

  const predictMutation = useMutation({
    mutationFn: async (transfer: Transfer) => {
      if (!projectSeason || !transfer.toClubId) {
        throw new Error('Missing destination club or project season')
      }
      return createPrediction({
        playerId: transfer.playerId,
        targetClubId: transfer.toClubId,
        seasonId: projectSeason.id,
        note: `transfer-project:${transfer.id}`,
      })
    },
    onSuccess: (prediction) => {
      setPendingId(null)
      void navigate(routes.predictionDetail(prediction.id))
    },
    onError: () => {
      setPendingId(null)
    },
  })

  return (
    <Stack spacing={3}>
      <PageHeader
        description="Latest transfer-window moves (Wikipedia-dated) for the upcoming campaign — project each signing at their new club using that club’s latest completed roster."
        eyebrow="Transfer board"
        title="Transfers"
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
        <FormControl sx={{ minWidth: 220 }}>
          <InputLabel id="transfer-season-label">Move season</InputLabel>
          <Select
            label="Move season"
            labelId="transfer-season-label"
            onChange={(event) => setSeasonId(event.target.value)}
            value={seasonId}
          >
            {seasons.map((season) => (
              <MenuItem key={season.id} value={season.id}>
                {season.label}
                {isUpcomingSeason(season.endDate) ? ' · current window' : ''}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
        <FormControl sx={{ minWidth: 220 }}>
          <InputLabel id="transfer-status-label">Status</InputLabel>
          <Select
            label="Status"
            labelId="transfer-status-label"
            onChange={(event) => setStatus(event.target.value as '' | TransferStatus)}
            value={status}
          >
            {STATUS_OPTIONS.map((option) => (
              <MenuItem key={option.label} value={option.value}>
                {option.label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      {predictMutation.isError ? (
        <Alert severity="error" variant="outlined">
          {userFacingErrorMessage(predictMutation.error)}
        </Alert>
      ) : null}

      <QueryState
        emptyDescription="Load the current window with scripts/ingest_transfers_from_wikipedia.py (or pick an older season that has inferred moves)."
        emptyTitle="No transfers for this season"
        error={transfersQuery.error}
        isEmpty={!transfersQuery.data?.content.length}
        isError={transfersQuery.isError}
        isLoading={transfersQuery.isLoading || seasonsQuery.isLoading}
        onRetry={() => void transfersQuery.refetch()}
      >
        <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Player</TableCell>
                <TableCell>From</TableCell>
                <TableCell>To</TableCell>
                <TableCell>Move season</TableCell>
                <TableCell>Type</TableCell>
                <TableCell align="right">Fee</TableCell>
                <TableCell align="right">Project</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {transfersQuery.data?.content.map((transfer) => {
                const canProject = Boolean(transfer.toClubId && projectSeason)
                const busy = pendingId === transfer.id && predictMutation.isPending
                return (
                  <TableRow hover key={transfer.id}>
                    <TableCell>
                      <MuiLink
                        component={RouterLink}
                        to={routes.playerDetail(transfer.playerId)}
                        underline="hover"
                      >
                        {transfer.playerName}
                      </MuiLink>
                      <Typography color="text.secondary" component="div" variant="caption">
                        {formatDate(transfer.transferDate)} · {transfer.status}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {transfer.fromClubId ? (
                        <MuiLink
                          component={RouterLink}
                          to={routes.clubDetail(transfer.fromClubId)}
                          underline="hover"
                        >
                          {transfer.fromClubName}
                        </MuiLink>
                      ) : (
                        (transfer.fromClubName ?? '—')
                      )}
                    </TableCell>
                    <TableCell>
                      {transfer.toClubId ? (
                        <MuiLink
                          component={RouterLink}
                          to={routes.clubDetail(transfer.toClubId)}
                          underline="hover"
                        >
                          {transfer.toClubName}
                        </MuiLink>
                      ) : (
                        (transfer.toClubName ?? '—')
                      )}
                    </TableCell>
                    <TableCell>{transfer.seasonLabel}</TableCell>
                    <TableCell>{transfer.type}</TableCell>
                    <TableCell align="right">{formatEur(transfer.feeEur)}</TableCell>
                    <TableCell align="right">
                      <Button
                        disabled={!canProject || predictMutation.isPending}
                        onClick={() => {
                          setPendingId(transfer.id)
                          predictMutation.mutate(transfer)
                        }}
                        size="small"
                        variant="contained"
                      >
                        {busy
                          ? 'Running…'
                          : projectSeason
                            ? `Predict ${projectSeason.label}`
                            : 'Predict'}
                      </Button>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </SurfaceCard>

        {(transfersQuery.data?.totalPages ?? 0) > 1 ? (
          <Pagination
            count={transfersQuery.data?.totalPages ?? 0}
            onChange={(_, value) => setPage(value - 1)}
            page={page + 1}
          />
        ) : null}
      </QueryState>
    </Stack>
  )
}
