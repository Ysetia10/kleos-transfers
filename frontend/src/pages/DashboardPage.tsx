import {
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
import { PageHeader } from '@/components/common/PageHeader'
import { QueryState } from '@/components/common/QueryState'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listPredictions } from '@/services/prediction/predictionApi'
import { formatDateTime, formatNumber } from '@/utils/format'

export function DashboardPage() {
  const query = useQuery({
    queryKey: queryKeys.predictions.list(0, 10),
    queryFn: () => listPredictions(0, 10),
  })

  return (
    <Stack spacing={4}>
      <PageHeader
        actions={
          <Button component={RouterLink} to={routes.prediction} variant="contained">
            New prediction
          </Button>
        }
        description="Recent transfer what-ifs and shortcuts into the identity catalogue."
        title="Dashboard"
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <Button component={RouterLink} to={routes.players} variant="outlined">
          Browse players
        </Button>
        <Button component={RouterLink} to={routes.clubs} variant="outlined">
          Browse clubs
        </Button>
        <Button component={RouterLink} to={routes.prediction} variant="outlined">
          Run prediction
        </Button>
      </Stack>

      <Stack spacing={2}>
        <Typography component="h2" variant="h3">
          Recent predictions
        </Typography>
        <QueryState
          emptyDescription="Run a player → club scenario to see it here."
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
                  <TableCell align="right">
                    {formatNumber(prediction.predictedMinutes)}
                  </TableCell>
                  <TableCell align="right">
                    {formatNumber(Number(prediction.compatibilityScore), 0)}
                  </TableCell>
                  <TableCell>{formatDateTime(prediction.createdAt)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </QueryState>
      </Stack>
    </Stack>
  )
}
