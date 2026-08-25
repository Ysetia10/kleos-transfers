import { Box, Container, Typography } from '@mui/material'

export function Footer() {
  return (
    <Box
      component="footer"
      sx={{
        borderTop: (theme) => `1px solid ${theme.palette.divider}`,
        py: 3,
        mt: 'auto',
      }}
    >
      <Container maxWidth="xl" sx={{ px: { xs: 2, sm: 3 } }}>
        <Typography color="text.secondary" sx={{ overflowWrap: 'anywhere' }} variant="body2">
          © {new Date().getFullYear()} Kleos Intelligence — Advanced Football Transfer Analytics
        </Typography>
      </Container>
    </Box>
  )
}
