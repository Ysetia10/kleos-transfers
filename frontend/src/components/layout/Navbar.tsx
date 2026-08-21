import {
  AppBar,
  Box,
  Button,
  Container,
  Drawer,
  IconButton,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import { NavLink as RouterNavLink } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { navigationItems, routes } from '@/constants/routes'

function MenuIcon() {
  return (
    <svg fill="none" height="20" viewBox="0 0 24 24" width="20" xmlns="http://www.w3.org/2000/svg">
      <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeLinecap="round" strokeWidth="1.8" />
    </svg>
  )
}

export function Navbar() {
  const [open, setOpen] = useState(false)

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
              gap: 1.25,
              textDecoration: 'none',
            }}
            to={routes.home}
          >
            <BrandMark animated size={30} />
            <Typography
              sx={{
                fontWeight: 500,
                letterSpacing: '0.06em',
                textTransform: 'uppercase',
                fontSize: '0.95rem',
              }}
              variant="h4"
            >
              Kleos
            </Typography>
          </Box>

          <Box
            aria-label="Primary navigation"
            component="nav"
            sx={{
              display: { xs: 'none', md: 'flex' },
              alignItems: 'center',
              gap: 0.5,
              ml: 1,
            }}
          >
            {navigationItems.map(({ label, to }) => (
              <Button
                component={RouterNavLink}
                end={to === routes.home}
                key={to}
                sx={(theme) => ({
                  color: theme.palette.text.secondary,
                  borderRadius: 1,
                  fontWeight: 500,
                  px: 1.75,
                  py: 0.75,
                  minWidth: 0,
                  '&.active': {
                    color: theme.palette.primary.main,
                    backgroundColor:
                      theme.palette.mode === 'dark'
                        ? 'rgba(59, 130, 246, 0.16)'
                        : 'rgba(37, 99, 235, 0.1)',
                  },
                })}
                to={to}
              >
                {label}
              </Button>
            ))}
          </Box>

          <Stack direction="row" spacing={1} sx={{ alignItems: 'center', ml: 'auto' }}>
            <ThemeToggle />
            <IconButton
              aria-label="Open navigation"
              onClick={() => setOpen(true)}
              sx={{ display: { md: 'none' }, color: 'text.secondary' }}
            >
              <MenuIcon />
            </IconButton>
          </Stack>
        </Toolbar>
      </Container>

      <Drawer anchor="right" onClose={() => setOpen(false)} open={open}>
        <Stack spacing={1} sx={{ minWidth: 260, p: 2.5 }}>
          <Typography sx={{ mb: 1 }} variant="caption">
            Navigate
          </Typography>
          {navigationItems.map(({ label, to }) => (
            <Button
              component={RouterNavLink}
              end={to === routes.home}
              key={to}
              onClick={() => setOpen(false)}
              sx={{ justifyContent: 'flex-start' }}
              to={to}
            >
              {label}
            </Button>
          ))}
        </Stack>
      </Drawer>
    </AppBar>
  )
}
