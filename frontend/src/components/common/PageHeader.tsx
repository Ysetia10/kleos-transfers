import { Box, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      sx={{ alignItems: { sm: 'flex-end' }, justifyContent: 'space-between', marginBottom: 4 }}
    >
      <Box>
        <Typography
          component="h1"
          sx={{ animation: 'kleos-rise 480ms ease both' }}
          variant="h1"
        >
          {title}
        </Typography>
        {description ? (
          <Typography
            color="text.secondary"
            sx={{ animation: 'kleos-rise 560ms ease both', marginTop: 1, maxWidth: 640 }}
            variant="body1"
          >
            {description}
          </Typography>
        ) : null}
      </Box>
      {actions}
    </Stack>
  )
}
