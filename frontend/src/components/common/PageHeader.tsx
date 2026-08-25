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
      sx={{
        alignItems: { xs: 'stretch', md: 'flex-start' },
        justifyContent: 'space-between',
        width: '100%',
        minWidth: 0,
      }}
    >
      <Stack
        direction="row"
        spacing={{ xs: 1.5, sm: 2 }}
        sx={{ alignItems: 'center', minWidth: 0, flex: 1 }}
      >
        {leading}
        <Stack spacing={0.75} sx={{ minWidth: 0, flex: 1 }}>
          {eyebrow ? (
            <Typography color="primary.main" sx={eyebrowSx} variant="caption">
              {eyebrow}
            </Typography>
          ) : null}
          {title != null && title !== '' ? (
            <Typography
              component="h1"
              sx={{
                overflowWrap: 'anywhere',
                wordBreak: 'break-word',
                fontSize: { xs: '1.65rem', sm: '1.9rem', md: '2.25rem' },
              }}
              variant="h1"
            >
              {title}
            </Typography>
          ) : null}
          {description ? (
            <Typography
              color="text.secondary"
              sx={{ overflowWrap: 'anywhere', ...descriptionSx }}
              variant="bodyLarge"
            >
              {description}
            </Typography>
          ) : null}
        </Stack>
      </Stack>
      {actions ? (
        <Stack
          direction="row"
          spacing={1}
          sx={{
            alignItems: 'center',
            flexShrink: 0,
            flexWrap: 'wrap',
            justifyContent: { xs: 'stretch', md: 'flex-end' },
            width: { xs: '100%', md: 'auto' },
            '& > *': {
              flex: { xs: '1 1 auto', sm: '0 0 auto' },
              minHeight: 44,
            },
          }}
        >
          {actions}
        </Stack>
      ) : null}
    </Stack>
  )
}
