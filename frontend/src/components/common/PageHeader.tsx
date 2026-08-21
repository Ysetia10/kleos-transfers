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
  descriptionSx?: object
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
  leading,
  eyebrowSx,
  descriptionSx,
}: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      spacing={2}
      sx={{ alignItems: { md: 'flex-start' }, justifyContent: 'space-between' }}
    >
      <Stack direction="row" spacing={2} sx={{ alignItems: 'center', minWidth: 0, flex: 1 }}>
        {leading}
        <Stack spacing={0.75} sx={{ minWidth: 0, maxWidth: '100%' }}>
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
            <Typography color="text.secondary" sx={descriptionSx} variant="bodyLarge">
              {description}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
      {actions ? <Stack direction="row" spacing={1}>{actions}</Stack> : null}
    </Stack>
  )
}
