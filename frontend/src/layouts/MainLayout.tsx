import { Box, Container } from '@mui/material'
import { Outlet } from 'react-router-dom'
import { Footer } from '@/components/layout/Footer'
import { Navbar } from '@/components/layout/Navbar'

export function MainLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <Container
        component="main"
        maxWidth="xl"
        sx={(theme) => ({
          flexGrow: 1,
          paddingBottom: theme.spacing(8),
          paddingTop: theme.spacing(4),
        })}
      >
        <Outlet />
      </Container>
      <Footer />
    </Box>
  )
}
