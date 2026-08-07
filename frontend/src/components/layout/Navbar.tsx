import { AppBar, Box, Button, Container, Stack, Toolbar, Typography } from '@mui/material'
import { NavLink as RouterNavLink } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { navigationItems, routes } from '@/constants/routes'

export function Navbar() {
  return (
    <AppBar component="header" position="sticky">
      <Container maxWidth="xl">
        <Toolbar disableGutters sx={{ gap: 2, minHeight: 68 }}>
          <Box
            component={RouterNavLink}
            sx={{
              alignItems: 'center',
              color: 'text.primary',
              display: 'inline-flex',
              flexGrow: 1,
              gap: 1.25,
              textDecoration: 'none',
              '&:hover .kleos-brand-word': { color: 'accent.main' },
            }}
            to={routes.home}
          >
            <BrandMark animated size={30} />
            <Typography className="kleos-brand-word" sx={{ transition: 'color 160ms ease' }} variant="h4">
              Kleos Transfers
            </Typography>
          </Box>

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
                  '&.active': { color: theme.palette.accent.dark },
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
