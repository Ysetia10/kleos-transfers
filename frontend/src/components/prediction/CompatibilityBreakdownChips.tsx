import { Box, Typography } from '@mui/material'
import type { CompatibilityBreakdown } from '@/types/domain'
import { formatNumber } from '@/utils/format'

const DIMENSIONS: Array<{ key: keyof CompatibilityBreakdown; label: string }> = [
  { key: 'system', label: 'System' },
  { key: 'role', label: 'Role' },
  { key: 'tempo', label: 'Tempo' },
  { key: 'league', label: 'League' },
  { key: 'manager', label: 'Manager' },
]

function tone(value: number): 'success.main' | 'warning.main' | 'error.main' | 'text.secondary' {
  if (value >= 70) return 'success.main'
  if (value >= 45) return 'warning.main'
  if (value > 0) return 'error.main'
  return 'text.secondary'
}

export function CompatibilityBreakdownChips({
  breakdown,
}: {
  breakdown: CompatibilityBreakdown | null | undefined
}) {
  if (!breakdown) {
    return (
      <Typography color="text.secondary" variant="body2">
        Dimensional fit scores are available on new predictions.
      </Typography>
    )
  }

  return (
    <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
      {DIMENSIONS.map((dimension) => {
        const value = Number(breakdown[dimension.key])
        return (
          <Box
            key={dimension.key}
            sx={{
              border: '1px solid',
              borderColor: 'divider',
              px: 1.25,
              py: 0.75,
              minWidth: 88,
            }}
          >
            <Typography color="text.secondary" variant="caption">
              {dimension.label}
            </Typography>
            <Typography color={tone(value)} sx={{ display: 'block', fontWeight: 700 }} variant="body2">
              {formatNumber(value, 0)}
            </Typography>
          </Box>
        )
      })}
    </Box>
  )
}
