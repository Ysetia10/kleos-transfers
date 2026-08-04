import { Box, Typography } from '@mui/material'

export function Footer() {
  return (
    <Box component="footer" sx={(theme) => ({ borderTop: `1px solid ${theme.palette.divider}`, padding: theme.spacing(3) })}>
      <Typography align="center" color="text.secondary" variant="caption">
        Kleos Transfers
      </Typography>
    </Box>
  )
}
