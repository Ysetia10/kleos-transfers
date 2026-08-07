import { useState } from 'react'
import {
  AppBar,
  Box,
  Button,
  Container,
  Divider,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography,
} from '@mui/material'
import { NavLink as RouterNavLink } from 'react-router-dom'
import { navigationItems, routes } from '@/constants/routes'

const drawerWidth = 280

function MenuIcon() {
  return (
    <Box
      aria-hidden
      component="span"
      sx={{
        display: 'inline-flex',
        flexDirection: 'column',
        gap: '5px',
        width: 18,
      }}
    >
      <Box sx={{ bgcolor: 'currentColor', height: 2, width: 1 }} />
      <Box sx={{ bgcolor: 'currentColor', height: 2, width: 1 }} />
      <Box sx={{ bgcolor: 'currentColor', height: 2, width: 1 }} />
    </Box>
  )
}

export function Navbar() {
  const [mobileOpen, setMobileOpen] = useState(false)

  const closeDrawer = () => setMobileOpen(false)

  return (
    <AppBar component="header" position="static">
      <Container maxWidth="xl">
        <Toolbar disableGutters sx={{ gap: 1, minHeight: 64 }}>
          <IconButton
            aria-controls="mobile-navigation-drawer"
            aria-expanded={mobileOpen}
            aria-label="Open navigation menu"
            edge="start"
            onClick={() => setMobileOpen(true)}
            sx={{ display: { xs: 'inline-flex', md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>

          <Typography
            color="text.primary"
            component={RouterNavLink}
            sx={{ flexGrow: { xs: 1, md: 0 }, textDecoration: 'none' }}
            to={routes.home}
            variant="h4"
          >
            Kleos Transfers
          </Typography>

          <Box
            component="nav"
            aria-label="Primary navigation"
            sx={(theme) => ({
              display: { xs: 'none', md: 'flex' },
              flexWrap: 'wrap',
              gap: theme.spacing(1),
              marginLeft: theme.spacing(4),
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

      <Drawer
        ModalProps={{ keepMounted: true }}
        anchor="left"
        id="mobile-navigation-drawer"
        onClose={closeDrawer}
        open={mobileOpen}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
        }}
      >
        <Box
          component="nav"
          aria-label="Mobile primary navigation"
          role="presentation"
          sx={{ display: 'flex', flexDirection: 'column', height: 1, pt: 2 }}
        >
          <Typography sx={{ px: 2 }} variant="h5">
            Kleos Transfers
          </Typography>
          <Divider sx={{ my: 2 }} />
          <List disablePadding>
            {navigationItems.map(({ label, to }) => (
              <ListItemButton
                component={RouterNavLink}
                end={to === routes.home}
                key={to}
                onClick={closeDrawer}
                sx={(theme) => ({
                  '&.active': {
                    bgcolor: theme.palette.action.selected,
                    color: theme.palette.text.primary,
                  },
                  color: theme.palette.text.secondary,
                })}
                to={to}
              >
                <ListItemText primary={label} />
              </ListItemButton>
            ))}
          </List>
        </Box>
      </Drawer>
    </AppBar>
  )
}
