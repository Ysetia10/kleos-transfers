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
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listPredictions } from '@/services/prediction/predictionApi'
import { formatDateTime, formatNumber } from '@/utils/format'

export function RecentPredictionsSection() {
  const query = useQuery({
    queryKey: queryKeys.predictions.list(0, 8),
    queryFn: () => listPredictions(0, 8),
  })

  return (
    <Stack spacing={2}>
      <Typography variant="h3">Recent predictions</Typography>
      <Typography color="text.secondary" variant="body2">
        Latest player → club scenarios with projected minutes and fit.
      </Typography>
      <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
        <QueryState
          emptyDescription="Run a scenario above — results will show up here."
          emptyTitle="No predictions yet"
          error={query.error}
          isEmpty={!!query.data?.empty}
          isError={query.isError}
          isLoading={query.isLoading}
          onRetry={() => void query.refetch()}
        >
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Scenario</TableCell>
                <TableCell>Season</TableCell>
                <TableCell align="right">Minutes</TableCell>
                <TableCell align="right">Compatibility</TableCell>
                <TableCell>Created</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {query.data?.content.map((prediction) => (
                <TableRow hover key={prediction.id}>
                  <TableCell>
                    <MuiLink
                      component={RouterLink}
                      to={routes.predictionDetail(prediction.id)}
                      underline="hover"
                    >
                      {prediction.playerName} → {prediction.targetClubName}
                    </MuiLink>
                  </TableCell>
                  <TableCell>{prediction.seasonLabel}</TableCell>
                  <TableCell align="right">{formatNumber(prediction.predictedMinutes)}</TableCell>
                  <TableCell align="right">
                    {formatNumber(Number(prediction.compatibilityScore), 0)}
                  </TableCell>
                  <TableCell>{formatDateTime(prediction.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </QueryState>
      </SurfaceCard>
    </Stack>
  )
}
