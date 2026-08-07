import { Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { homePredictPath, routes } from '@/constants/routes'

export function AboutPage() {
  return (
    <Stack spacing={4} sx={{ maxWidth: 720 }}>
      <PageHeader
        actions={
          <Button component={RouterLink} to={homePredictPath()} variant="contained">
            Open workspace
          </Button>
        }
        description="Open-source, explainable football transfer predictions."
        title="About"
      />
      <Typography color="text.secondary" variant="body1">
        Kleos Transfers estimates how a player is likely to perform after joining a club —
        projected minutes, contribution, and fit — with factor-level explanations rather than a
        black-box score.
      </Typography>
      <Typography color="text.secondary" variant="body1">
        The Home workspace holds prediction, recent runs, and the player/club catalogue in one
        scroll. Detail pages stay available for individual identities and prediction results.
      </Typography>
      <Button
        component={RouterLink}
        sx={{ alignSelf: 'flex-start' }}
        to={routes.home}
        variant="outlined"
      >
        Back to Home
      </Button>
    </Stack>
  )
}
