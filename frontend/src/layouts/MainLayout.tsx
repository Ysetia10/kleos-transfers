import { Box, Container } from '@mui/material'
import { Outlet } from 'react-router-dom'
import { ApiWakeBanner } from '@/components/layout/ApiWakeBanner'
import { Footer } from '@/components/layout/Footer'
import { Navbar } from '@/components/layout/Navbar'

export function MainLayout() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        maxWidth: '100vw',
        overflowX: 'clip',
        backgroundColor: 'background.default',
      }}
    >
      <Navbar />
      <ApiWakeBanner />
      <Box sx={{ flexGrow: 1, minWidth: 0, width: '100%' }}>
        <Container
          component="main"
          maxWidth="xl"
          sx={(theme) => ({
            paddingBottom: { xs: theme.spacing(5), md: theme.spacing(8) },
            paddingTop: { xs: theme.spacing(2.5), md: theme.spacing(4) },
            px: { xs: 2, sm: 3 },
            animation: 'kleos-rise 420ms ease both',
            minWidth: 0,
          })}
        >
          <Outlet />
        </Container>
      </Box>
      <Footer />
    </Box>
  )
}
