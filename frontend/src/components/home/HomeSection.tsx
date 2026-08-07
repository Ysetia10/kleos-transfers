import { Box, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface HomeSectionProps {
  id: string
  title: string
  description?: string
  children: ReactNode
  actions?: ReactNode
  /** Visual weight: hero panel vs quieter band */
  tone?: 'panel' | 'quiet'
}

export function HomeSection({
  id,
  title,
  description,
  children,
  actions,
  tone = 'panel',
}: HomeSectionProps) {
  return (
    <Box
      component="section"
      id={id}
      sx={(theme) => ({
        scrollMarginTop: 96,
        animation: 'kleos-rise 560ms ease both',
        ...(tone === 'panel'
          ? {
              backgroundColor: theme.palette.background.paper,
              border: `1px solid ${theme.palette.divider}`,
              borderRadius: 12,
              boxShadow: `0 18px 40px rgba(15, 36, 28, 0.06)`,
              overflow: 'hidden',
              position: 'relative',
              px: { xs: 2.5, md: 3.5 },
              py: { xs: 3, md: 4 },
              '&::before': {
                content: '""',
                position: 'absolute',
                left: 0,
                top: 0,
                bottom: 0,
                width: 4,
                background: `linear-gradient(180deg, ${theme.palette.accent.main}, ${theme.palette.primary.main})`,
              },
            }
          : {
              px: { xs: 0.5, md: 1 },
              py: 1,
            }),
      })}
    >
      <Stack spacing={2.5}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1.5}
          sx={{ alignItems: { sm: 'flex-end' }, justifyContent: 'space-between' }}
        >
          <Stack spacing={0.75} sx={{ maxWidth: 760 }}>
            <Typography component="h2" variant="h2">
              {title}
            </Typography>
            {description ? (
              <Typography color="text.secondary" variant="body1">
                {description}
              </Typography>
            ) : null}
          </Stack>
          {actions}
        </Stack>
        {children}
      </Stack>
    </Box>
  )
}
