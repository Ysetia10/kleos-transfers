import { Box, Link as MuiLink, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { routes } from '@/constants/routes'

export function Footer() {
  return (
    <Box
      component="footer"
      sx={(theme) => ({
        borderTop: `1px solid ${theme.palette.divider}`,
        padding: theme.spacing(3),
      })}
    >
      <Typography align="center" color="text.secondary" variant="caption">
        <MuiLink component={RouterLink} color="inherit" to={routes.home} underline="hover">
          Kleos Transfers
        </MuiLink>
        {' · '}
        <MuiLink component={RouterLink} color="inherit" to={routes.about} underline="hover">
          About
        </MuiLink>
      </Typography>
    </Box>
  )
}
