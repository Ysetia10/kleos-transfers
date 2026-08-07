import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material'
import { NavLink as RouterNavLink } from 'react-router-dom'
import { navigationItems, routes } from '@/constants/routes'

export function Navbar() {
  return (
    <AppBar component="header" position="static">
      <Container maxWidth="xl">
        <Toolbar disableGutters sx={{ gap: 2, minHeight: 64 }}>
          <Typography
            color="text.primary"
            component={RouterNavLink}
            sx={{ flexGrow: 1, textDecoration: 'none' }}
            to={routes.home}
            variant="h4"
          >
            Kleos Transfers
          </Typography>

          <Box
            component="nav"
            aria-label="Primary navigation"
            sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
          >
            {navigationItems.map(({ label, to }) => (
              <Button
                component={RouterNavLink}
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
