import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { routes } from '@/constants/routes'

export function NotFoundPage() {
  return (
    <Stack spacing={2}>
      <Typography component="h1" variant="h1">
        Page not found
      </Typography>
      <Typography color="text.secondary" variant="body1">
        The page you requested does not exist or has moved.
      </Typography>
      <Button component={RouterLink} to={routes.home} variant="contained" sx={{ alignSelf: 'flex-start' }}>
        Back to home
      </Button>
    </Stack>
  )
}
