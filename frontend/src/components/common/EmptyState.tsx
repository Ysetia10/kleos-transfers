import { Box, Typography } from '@mui/material'

interface EmptyStateProps {
  title: string
  description?: string
}

export function EmptyState({ title, description }: EmptyStateProps) {
  return (
    <Box
      sx={{
        border: (theme) => `1px dashed ${theme.palette.divider}`,
        borderRadius: 1,
        px: 3,
        py: 6,
        textAlign: 'center',
      }}
    >
      <Typography component="h2" variant="h3">
        {title}
      </Typography>
      {description ? (
        <Typography color="text.secondary" sx={{ marginTop: 1 }} variant="body1">
          {description}
        </Typography>
      ) : null}
    </Box>
  )
}
