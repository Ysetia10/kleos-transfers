import {
  Link as MuiLink,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { QueryState } from '@/components/common/QueryState'
import { ScrollableTable } from '@/components/common/ScrollableTable'
import { TransferBadge } from '@/components/transfer/TransferBadge'
import { routes } from '@/constants/routes'
import type { PlayerSeason } from '@/types/domain'
import { formatNumber } from '@/utils/format'

interface SquadTableProps {
  squad: PlayerSeason[] | undefined
  isLoading: boolean
  isError: boolean
  error: unknown
  onRetry: () => void
  emptyTitle?: string
  emptyDescription?: string
}

export function SquadTable({
  squad,
  isLoading,
  isError,
  error,
  onRetry,
  emptyTitle = 'No squad rows',
  emptyDescription = 'No player-seasons recorded for this club and season.',
}: SquadTableProps) {
  return (
    <QueryState
      emptyDescription={emptyDescription}
      emptyTitle={emptyTitle}
      error={error}
      isEmpty={!squad?.length}
      isError={isError}
      isLoading={isLoading}
      onRetry={onRetry}
    >
      <Typography color="text.secondary" sx={{ mb: 1, px: { xs: 2, md: 0 } }} variant="body2">
        {squad?.length ?? 0} players · sorted by minutes
      </Typography>
      <ScrollableTable minWidth={480}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Player</TableCell>
              <TableCell>Pos</TableCell>
              <TableCell align="right">Apps</TableCell>
              <TableCell align="right">Mins</TableCell>
              <TableCell align="right">G</TableCell>
              <TableCell align="right">A</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {squad?.map((row) => (
              <TableRow hover key={row.id}>
                <TableCell>
                  <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center', minWidth: 0 }}>
                    <MuiLink
                      component={RouterLink}
                      sx={{ overflowWrap: 'anywhere' }}
                      to={routes.playerDetail(row.playerId)}
                      underline="hover"
                    >
                      {row.playerName}
                    </MuiLink>
                    {row.inboundTransfer ? <TransferBadge transfer={row.inboundTransfer} /> : null}
                  </Stack>
                </TableCell>
                <TableCell>{row.primaryPosition}</TableCell>
                <TableCell align="right">{formatNumber(row.appearances)}</TableCell>
                <TableCell align="right">{formatNumber(row.minutesPlayed)}</TableCell>
                <TableCell align="right">{formatNumber(row.goals)}</TableCell>
                <TableCell align="right">{formatNumber(row.assists)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </ScrollableTable>
    </QueryState>
  )
}
