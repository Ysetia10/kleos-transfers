import { Box, Button, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
  action?: ReactNode
}

export function EmptyState({ title, description, actionLabel, onAction, action }: EmptyStateProps) {
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
      {action
        ? action
        : actionLabel && onAction
          ? (
              <Button onClick={onAction} sx={{ marginTop: 3 }} variant="contained">
                {actionLabel}
              </Button>
            )
          : null}
    </Box>
  )
}
