import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { routes } from '@/constants/routes'

export function HomePage() {
  return (
    <Stack spacing={3} sx={{ maxWidth: 720 }}>
      <Typography component="h1" variant="h1">
        Kleos Transfers
      </Typography>
      <Typography color="text.secondary" variant="body1">
        Context-aware football transfer predictions. Estimate how a player is likely to perform
        after a move — with explainable factors, not a black box.
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <Button component={RouterLink} to={routes.prediction} variant="contained">
          Run a prediction
        </Button>
        <Button component={RouterLink} to={routes.dashboard} variant="outlined">
          Open dashboard
        </Button>
      </Stack>
    </Stack>
  )
}
