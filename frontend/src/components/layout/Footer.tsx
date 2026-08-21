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
      <Container maxWidth="xl">
        <Typography color="text.secondary" variant="body2">
          © {new Date().getFullYear()} Kleos Intelligence — Advanced Football Transfer Analytics
        </Typography>
      </Container>
    </Box>
  )
}
