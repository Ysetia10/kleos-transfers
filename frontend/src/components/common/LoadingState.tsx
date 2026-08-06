import { Box, CircularProgress, Typography } from '@mui/material'

interface LoadingStateProps {
  label?: string
}

export function LoadingState({ label = 'Loading…' }: LoadingStateProps) {
  return (
    <Box
      aria-busy="true"
      aria-live="polite"
      sx={{
        alignItems: 'center',
        display: 'flex',
        flexDirection: 'column',
        gap: 2,
        justifyContent: 'center',
        minHeight: 200,
        py: 6,
      }}
    >
      <CircularProgress size={32} />
      <Typography color="text.secondary" variant="body2">
        {label}
      </Typography>
    </Box>
  )
}
