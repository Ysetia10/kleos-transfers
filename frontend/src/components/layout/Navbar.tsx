import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material'
import { NavLink as RouterNavLink } from 'react-router-dom'
import { navigationItems, routes } from '@/constants/routes'

export function Navbar() {
  return (
    <AppBar component="header" position="static">
      <Container maxWidth="xl">
        <Toolbar disableGutters sx={{ flexWrap: 'wrap' }}>
          <Typography
            color="text.primary"
            component={RouterNavLink}
            sx={{ textDecoration: 'none' }}
            to={routes.home}
            variant="h4"
          >
            Kleos Transfers
          </Typography>
          <Box
            component="nav"
            aria-label="Primary navigation"
            sx={(theme) => ({
              display: 'flex',
              flexWrap: 'wrap',
              gap: theme.spacing(1),
              marginLeft: { md: theme.spacing(4) },
              marginTop: { xs: theme.spacing(2), md: theme.spacing(0) },
              width: { xs: '100%', md: 'auto' },
            })}
          >
            {navigationItems.map(({ label, to }) => (
              <Button
                component={RouterNavLink}
                end={to === routes.home}
                key={to}
                sx={(theme) => ({
                  '&.active': { color: theme.palette.text.primary },
                  color: theme.palette.text.secondary,
                })}
                to={to}
              >
                {label}
              </Button>
            ))}
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  )
}
