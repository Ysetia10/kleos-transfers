import { Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface PageHeaderProps {
  eyebrow?: string
  /** When omitted, only eyebrow + description show (no large title). */
  title?: ReactNode
  description?: ReactNode
  actions?: ReactNode
  leading?: ReactNode
  /** Extra styles for the eyebrow (e.g. larger section label). */
  eyebrowSx?: object
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
  leading,
  eyebrowSx,
}: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      spacing={2}
      sx={{ alignItems: { md: 'flex-start' }, justifyContent: 'space-between', mb: 1 }}
    >
      <Stack direction="row" spacing={2} sx={{ alignItems: 'center', maxWidth: 820 }}>
        {leading}
        <Stack spacing={0.75} sx={{ minWidth: 0 }}>
          {eyebrow ? (
            <Typography color="primary.main" sx={eyebrowSx} variant="caption">
              {eyebrow}
            </Typography>
          ) : null}
          {title != null && title !== '' ? (
            <Typography component="h1" variant="h1">
              {title}
            </Typography>
          ) : null}
          {description ? (
            <Typography color="text.secondary" variant="bodyLarge">
              {description}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
      {actions ? <Stack direction="row" spacing={1}>{actions}</Stack> : null}
    </Stack>
  )
}
