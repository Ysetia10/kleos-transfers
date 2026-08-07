import { Box } from '@mui/material'

interface BrandMarkProps {
  size?: number
  animated?: boolean
}

/** Green bolt mark used across the analytics shell. */
export function BrandMark({ size = 28, animated = false }: BrandMarkProps) {
  return (
    <Box
      aria-hidden
      component="span"
      sx={{
        display: 'inline-flex',
        width: size,
        height: size,
        flexShrink: 0,
        animation: animated ? 'kleos-mark-in 520ms ease both' : undefined,
        color: 'accent.main',
      }}
    >
      <svg fill="none" height={size} viewBox="0 0 32 32" width={size} xmlns="http://www.w3.org/2000/svg">
        <rect fill="currentColor" height="32" rx="9" width="32" />
        <path
          d="M17.8 6.5 10 17.2h5.1l-1.2 8.3L22 14.8h-5.2l1-8.3Z"
          fill="#0B0E14"
        />
      </svg>
    </Box>
  )
}
