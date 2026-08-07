import { Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'

interface HomeSectionProps {
  id: string
  title: string
  description?: string
  children: ReactNode
  actions?: ReactNode
}

export function HomeSection({ id, title, description, children, actions }: HomeSectionProps) {
  return (
    <Stack
      component="section"
      id={id}
      spacing={2.5}
      sx={{ scrollMarginTop: 96 }}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1.5}
        sx={{ alignItems: { sm: 'flex-end' }, justifyContent: 'space-between' }}
      >
        <Stack spacing={0.75} sx={{ maxWidth: 720 }}>
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
  )
}
