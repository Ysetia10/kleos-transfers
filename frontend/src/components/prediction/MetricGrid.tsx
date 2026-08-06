import { Box, Stack, Typography } from '@mui/material'
import { formatEur, formatNumber } from '@/utils/format'

interface MetricGridProps {
  minutes: number
  goals: number
  assists: number
  xg: number
  xa: number
  marketValueEur: number | null
}

export function MetricGrid({ minutes, goals, assists, xg, xa, marketValueEur }: MetricGridProps) {
  const metrics = [
    { label: 'Minutes', value: formatNumber(minutes) },
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
        gap: 2,
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
            borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
            pb: 1.5,
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
