import { Chip, Stack, Typography } from '@mui/material'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import type { Explanation, ExplanationDirection } from '@/types/domain'

interface ExplanationListProps {
  explanations: Explanation[]
}

const directionColor: Record<ExplanationDirection, 'success' | 'error' | 'default'> = {
  POSITIVE: 'success',
  NEGATIVE: 'error',
  NEUTRAL: 'default',
}

const barColor: Record<ExplanationDirection, string> = {
  POSITIVE: '#22C55E',
  NEGATIVE: '#F87171',
  NEUTRAL: '#64748B',
}

export function ExplanationList({ explanations }: ExplanationListProps) {
  const chartData = explanations.slice(0, 8).map((item) => ({
    name: item.label,
    impact: Number(item.impact),
    direction: item.direction,
  }))

  return (
    <Stack spacing={3}>
      {chartData.length > 0 ? (
        <Stack spacing={1}>
          <Typography variant="subtitle2">Factor impact</Typography>
          <ResponsiveContainer height={220} width="100%">
            <BarChart data={chartData} margin={{ bottom: 48, left: 0, right: 8, top: 8 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis
                angle={-25}
                dataKey="name"
                height={60}
                interval={0}
                textAnchor="end"
                tick={{ fontSize: 11 }}
              />
              <YAxis tick={{ fontSize: 11 }} width={32} />
              <Tooltip />
              <Bar dataKey="impact" radius={[4, 4, 0, 0]}>
                {chartData.map((entry) => (
                  <Cell fill={barColor[entry.direction]} key={entry.name} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Stack>
      ) : null}

      <Stack component="ol" spacing={1.5} sx={{ listStyle: 'none', m: 0, p: 0 }}>
        {explanations.map((item) => (
          <Stack
            component="li"
            key={item.id}
            spacing={1}
            sx={{
              border: (theme) => `1px solid ${theme.palette.divider}`,
              borderLeft: (theme) =>
                `3px solid ${
                  item.direction === 'POSITIVE'
                    ? theme.palette.success.main
                    : item.direction === 'NEGATIVE'
                      ? theme.palette.error.main
                      : theme.palette.divider
                }`,
              borderRadius: 2,
              p: 1.5,
            }}
          >
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography sx={{ fontWeight: 600 }} variant="subtitle2">
                {item.label}
              </Typography>
              <Chip
                color={directionColor[item.direction]}
                label={item.direction.toLowerCase()}
                size="small"
                variant="outlined"
              />
            </Stack>
            <Typography color="text.secondary" variant="body2">
              {item.detail}
            </Typography>
            <Typography color="text.secondary" variant="body2">
              Impact {Number(item.impact).toFixed(1)}
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  )
}
