import { Box, type BoxProps } from '@mui/material'
import type { PropsWithChildren } from 'react'

type SurfaceCardProps = PropsWithChildren<BoxProps> & {
  accent?: 'default' | 'positive' | 'negative' | 'info'
}

export function SurfaceCard({ children, accent = 'default', sx, ...rest }: SurfaceCardProps) {
  return (
    <Box
      {...rest}
      sx={[
        {
          backgroundColor: 'background.paper',
          border: '1px solid',
          borderColor:
            accent === 'positive'
              ? 'success.main'
              : accent === 'negative'
                ? 'error.main'
                : accent === 'info'
                  ? 'primary.main'
                  : 'divider',
          borderRadius: 3,
          p: { xs: 2, md: 2.5 },
        },
        ...(Array.isArray(sx) ? sx : sx ? [sx] : []),
      ]}
    >
      {children}
    </Box>
  )
}
