import { Box, Stack, Typography } from '@mui/material'
import { formatEur, formatNumber } from '@/utils/format'

interface MetricGridProps {
  minutes: number
  minutesLow?: number | null
  minutesHigh?: number | null
  goals: number
  assists: number
  marketValueEur: number | null
}

export function MetricGrid({
  minutes,
  minutesLow,
  minutesHigh,
  goals,
  assists,
  marketValueEur,
}: MetricGridProps) {
  const hasRange =
    minutesLow != null && minutesHigh != null && (minutesLow !== minutes || minutesHigh !== minutes)
  const minutesLabel = hasRange
    ? `${formatNumber(minutes)} (${formatNumber(minutesLow)}–${formatNumber(minutesHigh)})`
    : formatNumber(minutes)

  const metrics = [
    { label: 'xM', value: minutesLabel, wide: hasRange },
    { label: 'Goals', value: formatNumber(goals, 1), wide: false },
    { label: 'Assists', value: formatNumber(assists, 1), wide: false },
    { label: 'Market value', value: formatEur(marketValueEur), wide: false },
  ]

  return (
    <Box
      sx={{
        display: 'grid',
        gap: 1.5,
        gridTemplateColumns: {
          xs: 'repeat(2, minmax(0, 1fr))',
          sm: 'repeat(2, minmax(0, 1fr))',
        },
      }}
    >
      {metrics.map((metric) => (
        <Stack
          key={metric.label}
          spacing={0.5}
          sx={{
            border: (theme) => `1px solid ${theme.palette.divider}`,
            borderRadius: 2,
            px: { xs: 1.25, sm: 1.5 },
            py: 1.25,
            minWidth: 0,
            gridColumn: metric.wide ? { xs: '1 / -1', sm: 'auto' } : 'auto',
            backgroundColor: (theme) =>
              theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(15,23,42,0.02)',
          }}
        >
          <Typography color="text.secondary" variant="caption">
            {metric.label}
          </Typography>
          <Typography
            component="p"
            sx={{
              fontSize: {
                xs: metric.wide ? '1.05rem' : '1.15rem',
                sm: '1.25rem',
              },
              overflowWrap: 'anywhere',
              wordBreak: 'break-word',
            }}
            variant="h3"
          >
            {metric.value}
          </Typography>
        </Stack>
      ))}
    </Box>
  )
}
