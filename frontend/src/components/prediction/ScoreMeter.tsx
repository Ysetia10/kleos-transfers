import { LinearProgress, Stack, Typography } from '@mui/material'

interface ScoreMeterProps {
  label: string
  value: number
  helper?: string
}

export function ScoreMeter({ label, value, helper }: ScoreMeterProps) {
  const clamped = Math.max(0, Math.min(100, value))
  const tone = clamped >= 70 ? 'success' : clamped >= 45 ? 'warning' : 'error'

  return (
    <Stack spacing={1}>
      <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
        <Typography variant="subtitle2">{label}</Typography>
        <Typography sx={{ fontWeight: 600 }} variant="subtitle2">
          {Math.round(clamped)}
        </Typography>
      </Stack>
      <LinearProgress
        color={tone}
        sx={{ borderRadius: 999, height: 8 }}
        value={clamped}
        variant="determinate"
      />
      {helper ? (
        <Typography color="text.secondary" variant="body2">
          {helper}
        </Typography>
      ) : null}
    </Stack>
  )
}
