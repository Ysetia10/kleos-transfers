import { Box, Container } from '@mui/material'
import { Outlet } from 'react-router-dom'
import { Footer } from '@/components/layout/Footer'
import { Navbar } from '@/components/layout/Navbar'

export function MainLayout() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        minHeight: '100vh',
        backgroundColor: 'background.default',
      }}
    >
      <Navbar />
      <Box sx={{ flexGrow: 1 }}>
        <Container
          component="main"
          maxWidth="xl"
          sx={(theme) => ({
            paddingBottom: theme.spacing(8),
            paddingTop: theme.spacing(4),
            animation: 'kleos-rise 420ms ease both',
          })}
        >
          <Outlet />
        </Container>
      </Box>
      <Footer />
    </Box>
  )
}
