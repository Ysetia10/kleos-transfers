import { Box, Container } from '@mui/material'
import { Outlet } from 'react-router-dom'
import { Footer } from '@/components/layout/Footer'
import { Navbar } from '@/components/layout/Navbar'

export function MainLayout() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <Box
        sx={{
          position: 'relative',
          flexGrow: 1,
          '&::before': {
            content: '""',
            pointerEvents: 'none',
            position: 'absolute',
            inset: 0,
            background: (theme) =>
              `radial-gradient(ellipse 80% 50% at 10% -10%, ${theme.palette.pitch.mist}, transparent 55%),
               radial-gradient(ellipse 60% 40% at 100% 0%, rgba(22, 53, 40, 0.08), transparent 50%)`,
          },
        }}
      >
        <Container
          component="main"
          maxWidth="xl"
          sx={(theme) => ({
            position: 'relative',
            paddingBottom: theme.spacing(10),
            paddingTop: theme.spacing(4),
          })}
        >
          <Outlet />
        </Container>
      </Box>
      <Footer />
    </Box>
  )
}
