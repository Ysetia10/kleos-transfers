import { Box, Container, Link as MuiLink, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { routes } from '@/constants/routes'

export function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        borderTop: (theme) => `1px solid ${theme.palette.divider}`,
        py: 3,
        mt: 'auto',
      }}
    >
      <Container maxWidth="xl">
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
        >
          <Typography color="text.secondary" variant="body2">
            Kleos Transfers · explainable football transfer intelligence · v0.2 heuristic
          </Typography>
          <Stack direction="row" spacing={2}>
            <MuiLink component={RouterLink} color="text.secondary" to={routes.home} underline="hover">
              Prediction
            </MuiLink>
            <MuiLink
              component={RouterLink}
              color="text.secondary"
              to={routes.methodology}
              underline="hover"
            >
              Methodology
            </MuiLink>
          </Stack>
        </Stack>
      </Container>
    </Box>
  )
}
