import { Box, Link as MuiLink, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { routes } from '@/constants/routes'

export function Footer() {
  return (
    <Box
      component="footer"
      sx={(theme) => ({
        borderTop: `1px solid ${theme.palette.divider}`,
        backgroundColor: 'rgba(251, 252, 251, 0.9)',
        px: 3,
        py: 2.5,
      })}
    >
      <Stack
        direction="row"
        spacing={1}
        sx={{ alignItems: 'center', color: 'text.secondary', justifyContent: 'center' }}
      >
        <BrandMark size={18} />
        <Typography variant="caption">
          <MuiLink component={RouterLink} color="inherit" to={routes.home} underline="hover">
            Kleos Transfers
          </MuiLink>
          {' · '}
          <MuiLink component={RouterLink} color="inherit" to={routes.about} underline="hover">
            About
          </MuiLink>
        </Typography>
      </Stack>
    </Box>
  )
}
