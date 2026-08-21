import { Box, Chip, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { QueryState } from '@/components/common/QueryState'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { queryKeys } from '@/services/api/queryKeys'
import { listInjuries } from '@/services/injury/injuryApi'
import type { Injury, InjurySeverity } from '@/types/domain'
import { formatDate, formatNumber } from '@/utils/format'

type InjuryHistorySectionProps = {
  playerId: string
}

const INFERRED_TYPE = 'Inferred availability gap'

function isInferred(injury: Injury): boolean {
  return injury.injuryType.trim().toLowerCase() === INFERRED_TYPE.toLowerCase()
}

function severityTone(severity: InjurySeverity): 'default' | 'warning' | 'error' {
  if (severity === 'SEVERE') {
    return 'error'
  }
  if (severity === 'MODERATE') {
    return 'warning'
  }
  return 'default'
}

function spellLabel(injury: Injury): string {
  if (injury.ongoing) {
    return `From ${formatDate(injury.startDate)} · ongoing`
  }
  if (injury.endDate) {
    return `${formatDate(injury.startDate)} – ${formatDate(injury.endDate)}`
  }
  return formatDate(injury.startDate)
}

function daysLabel(injury: Injury): string {
  if (injury.ongoing) {
    return 'Ongoing'
  }
  if (injury.daysOut == null) {
    return '—'
  }
  return `${formatNumber(injury.daysOut)} day${injury.daysOut === 1 ? '' : 's'}`
}

export function InjuryHistorySection({ playerId }: InjuryHistorySectionProps) {
  const query = useQuery({
    queryKey: queryKeys.injuries.byPlayer(playerId, 0, 30),
    queryFn: () => listInjuries(0, 30, { playerId }),
    enabled: !!playerId,
  })

  const injuries = query.data?.content ?? []
  const totalDays = injuries.reduce((sum, injury) => sum + (injury.daysOut ?? 0), 0)
  const ongoingCount = injuries.filter((injury) => injury.ongoing).length
  const inferredCount = injuries.filter(isInferred).length

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="h3">Availability history</Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
          Recorded injury spells used as minutes context. Inferred gaps are proxy signals from
          unexplained minutes collapses — not clinical diagnoses.
        </Typography>
      </Box>

      <QueryState
        emptyDescription="No injury spells on file for this player yet."
        emptyTitle="No availability interruptions logged"
        error={query.error}
        isEmpty={!injuries.length}
        isError={query.isError}
        isLoading={query.isLoading}
        onRetry={() => void query.refetch()}
      >
        <Stack spacing={2}>
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(3, minmax(0, 1fr))' },
            }}
          >
            <SurfaceCard>
              <Typography color="text.secondary" variant="caption">
                Spells on file
              </Typography>
              <Typography sx={{ mt: 1 }} variant="h3">
                {formatNumber(injuries.length)}
              </Typography>
            </SurfaceCard>
            <SurfaceCard>
              <Typography color="text.secondary" variant="caption">
                Documented days out
              </Typography>
              <Typography sx={{ mt: 1 }} variant="h3">
                {formatNumber(totalDays)}
              </Typography>
              {ongoingCount > 0 ? (
                <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
                  {ongoingCount} ongoing
                </Typography>
              ) : null}
            </SurfaceCard>
            <SurfaceCard>
              <Typography color="text.secondary" variant="caption">
                Source mix
              </Typography>
              <Typography sx={{ mt: 1 }} variant="h3">
                {inferredCount > 0
                  ? `${formatNumber(injuries.length - inferredCount)} confirmed`
                  : 'Confirmed'}
              </Typography>
              {inferredCount > 0 ? (
                <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
                  {formatNumber(inferredCount)} inferred
                </Typography>
              ) : (
                <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
                  No inferred gaps
                </Typography>
              )}
            </SurfaceCard>
          </Box>

          <Stack spacing={1.5}>
            {injuries.map((injury) => (
              <Box
                key={injury.id}
                sx={{
                  border: '1px solid',
                  borderColor: 'divider',
                  borderLeft: '3px solid',
                  borderLeftColor:
                    injury.severity === 'SEVERE'
                      ? 'error.main'
                      : injury.severity === 'MODERATE'
                        ? 'warning.main'
                        : 'divider',
                  borderRadius: 2,
                  p: 1.5,
                }}
              >
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={1}
                  sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
                >
                  <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                    <Typography sx={{ fontWeight: 500 }} variant="subtitle2">
                      {isInferred(injury) ? 'Availability gap' : injury.injuryType}
                    </Typography>
                    <Typography color="text.secondary" variant="body2">
                      {spellLabel(injury)}
                    </Typography>
                  </Stack>
                  <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', alignItems: 'center' }}>
                    <Chip
                      color={severityTone(injury.severity)}
                      label={injury.severity.toLowerCase()}
                      size="small"
                      variant="outlined"
                    />
                    {isInferred(injury) ? (
                      <Chip label="inferred" size="small" variant="outlined" />
                    ) : null}
                    {injury.ongoing ? (
                      <Chip color="warning" label="ongoing" size="small" variant="outlined" />
                    ) : null}
                    <Typography color="text.secondary" variant="body2">
                      {daysLabel(injury)}
                    </Typography>
                  </Stack>
                </Stack>
              </Box>
            ))}
          </Stack>
        </Stack>
      </QueryState>
    </Stack>
  )
}
