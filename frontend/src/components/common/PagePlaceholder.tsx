import { Box, Typography } from '@mui/material'

interface PagePlaceholderProps {
  title: string
}

export function PagePlaceholder({ title }: PagePlaceholderProps) {
  return (
    <Box component="section">
      <Typography component="h1" variant="h1">
        {title}
      </Typography>
    </Box>
  )
}
