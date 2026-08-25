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
import { QueryState } from '@/components/common/QueryState'
import { ScrollableTable } from '@/components/common/ScrollableTable'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { homePredictPath, routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listPredictions } from '@/services/prediction/predictionApi'
import type { Prediction } from '@/types/domain'
import { formatNumber } from '@/utils/format'

type ScenarioComparisonSectionProps = {
  playerId: string
}

/** Newest prediction first; keep one row per destination club. */
function latestPerClub(predictions: Prediction[]): Prediction[] {
  const seen = new Set<string>()
  const unique: Prediction[] = []
  for (const prediction of predictions) {
    if (seen.has(prediction.targetClubId)) {
      continue
    }
    seen.add(prediction.targetClubId)
    unique.push(prediction)
  }
  return unique
}

export function ScenarioComparisonSection({ playerId }: ScenarioComparisonSectionProps) {
  const query = useQuery({
    queryKey: queryKeys.predictions.list(0, 24, playerId),
    queryFn: () => listPredictions(0, 24, { playerId }),
    enabled: !!playerId,
  })

  const scenarios = query.data?.content ?? []
  const byClub = latestPerClub(scenarios)
  const comparable = byClub.slice(0, 4)

  return (
    <Stack spacing={2}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        sx={{ alignItems: { sm: 'flex-end' }, justifyContent: 'space-between' }}
      >
        <Box>
          <Typography variant="h3">Club scenario comparison</Typography>
          <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
            Side-by-side projections for destination clubs. Run another scenario to add a column.
          </Typography>
        </Box>
        <Button
          component={RouterLink}
          to={homePredictPath({ playerId })}
          variant="contained"
        >
          Build new scenario
        </Button>
      </Stack>

      <QueryState
        emptyDescription="Simulate this player at two or more clubs to compare minutes, output, and fit."
        emptyTitle="No scenarios yet"
        error={query.error}
        isEmpty={!scenarios.length}
        isError={query.isError}
        isLoading={query.isLoading}
        onRetry={() => void query.refetch()}
      >
        {comparable.length >= 2 ? (
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: {
                xs: '1fr',
                sm: `repeat(${Math.min(comparable.length, 2)}, minmax(0, 1fr))`,
                md: `repeat(${Math.min(comparable.length, 2)}, minmax(0, 1fr))`,
                lg: `repeat(${Math.min(comparable.length, 3)}, minmax(0, 1fr))`,
              },
            }}
          >
            {comparable.map((prediction) => (
              <SurfaceCard key={prediction.id} accent="info">
                <Typography color="primary.main" variant="caption">
                  {prediction.seasonLabel}
                </Typography>
                <Typography sx={{ mt: 0.5 }} variant="h4">
                  {prediction.targetClubName}
                </Typography>
                <Box
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: '1fr 1fr',
                    gap: 1.5,
                    mt: 2,
                  }}
                >
                  <Metric
                    label="Minutes"
                    value={formatNumber(prediction.predictedMinutes)}
                  />
                  <Metric
                    label="Range"
                    value={`${formatNumber(prediction.predictedMinutesLow)}–${formatNumber(prediction.predictedMinutesHigh)}`}
                  />
                  <Metric
                    label="Fit"
                    value={formatNumber(Number(prediction.compatibilityScore), 0)}
                  />
                  <Metric
                    label="Goals / Assists"
                    value={`${formatNumber(Number(prediction.predictedGoals), 1)} / ${formatNumber(Number(prediction.predictedAssists), 1)}`}
                  />
                  <Metric
                    label="Confidence"
                    value={formatNumber(Number(prediction.confidenceScore), 0)}
                  />
                </Box>
                <Button
                  component={RouterLink}
                  size="small"
                  sx={{ mt: 2 }}
                  to={routes.predictionDetail(prediction.id)}
                  variant="outlined"
                >
                  Open result
                </Button>
              </SurfaceCard>
            ))}
          </Box>
        ) : comparable[0] ? (
          <SurfaceCard accent="info">
            <Typography variant="h4">{comparable[0].targetClubName}</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              One destination so far. Build another club scenario to compare projections side by
              side.
            </Typography>
            <Button
              component={RouterLink}
              sx={{ mt: 2, mr: 1 }}
              to={routes.predictionDetail(comparable[0].id)}
              variant="outlined"
            >
              Open result
            </Button>
            <Button
              component={RouterLink}
              sx={{ mt: 2 }}
              to={homePredictPath({ playerId })}
              variant="contained"
            >
              Add club scenario
            </Button>
          </SurfaceCard>
        ) : null}

        {byClub.length > 0 ? (
          <SurfaceCard sx={{ p: 0, overflow: 'hidden' }}>
            <ScrollableTable minWidth={480}>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Club</TableCell>
                    <TableCell>Season</TableCell>
                    <TableCell align="right">Minutes</TableCell>
                    <TableCell align="right">Fit</TableCell>
                    <TableCell align="right">Confidence</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {byClub.map((prediction) => (
                    <TableRow hover key={prediction.id}>
                      <TableCell>
                        <MuiLink
                          component={RouterLink}
                          sx={{ overflowWrap: 'anywhere' }}
                          to={routes.predictionDetail(prediction.id)}
                          underline="hover"
                        >
                          {prediction.targetClubName}
                        </MuiLink>
                      </TableCell>
                      <TableCell>{prediction.seasonLabel}</TableCell>
                      <TableCell align="right">
                        {formatNumber(prediction.predictedMinutes)}
                      </TableCell>
                      <TableCell align="right">
                        {formatNumber(Number(prediction.compatibilityScore), 0)}
                      </TableCell>
                      <TableCell align="right">
                        {formatNumber(Number(prediction.confidenceScore), 0)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </ScrollableTable>
          </SurfaceCard>
        ) : null}
      </QueryState>
    </Stack>
  )
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <Stack spacing={0.25}>
      <Typography color="text.secondary" variant="caption">
        {label}
      </Typography>
      <Typography variant="body2">{value}</Typography>
    </Stack>
  )
}
