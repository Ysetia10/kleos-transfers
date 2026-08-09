import { Box, Stack, Typography } from '@mui/material'
import { formatEur, formatNumber } from '@/utils/format'

interface MetricGridProps {
  minutes: number
  minutesLow?: number | null
  minutesHigh?: number | null
  goals: number
  assists: number
  xg: number
  xa: number
  marketValueEur: number | null
}

export function MetricGrid({
  minutes,
  minutesLow,
  minutesHigh,
  goals,
  assists,
  xg,
  xa,
  marketValueEur,
}: MetricGridProps) {
  const minutesLabel =
    minutesLow != null && minutesHigh != null && (minutesLow !== minutes || minutesHigh !== minutes)
      ? `${formatNumber(minutes)} (${formatNumber(minutesLow)}–${formatNumber(minutesHigh)})`
      : formatNumber(minutes)

  const metrics = [
    { label: 'xM', value: minutesLabel },
    { label: 'Goals', value: formatNumber(goals, 1) },
    { label: 'Assists', value: formatNumber(assists, 1) },
    { label: 'xG', value: formatNumber(xg, 1) },
    { label: 'xA', value: formatNumber(xa, 1) },
    { label: 'Market value', value: formatEur(marketValueEur) },
  ]

  return (
    <Box
      sx={{
        display: 'grid',
        gap: 1.5,
        gridTemplateColumns: {
          xs: 'repeat(2, minmax(0, 1fr))',
          md: 'repeat(3, minmax(0, 1fr))',
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
            px: 1.5,
            py: 1.25,
            backgroundColor: (theme) =>
              theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.02)' : 'rgba(15,23,42,0.02)',
          }}
        >
          <Typography color="text.secondary" variant="caption">
            {metric.label}
          </Typography>
          <Typography component="p" variant="h3">
            {metric.value}
          </Typography>
        </Stack>
      ))}
    </Box>
  )
}
