import { Box } from '@mui/material'

interface BrandMarkProps {
  size?: number
  animated?: boolean
}

/** Minimal transfer mark: pitch circle + directional swap — no emoji clutter. */
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
      }}
    >
      <svg fill="none" height={size} viewBox="0 0 32 32" width={size} xmlns="http://www.w3.org/2000/svg">
        <circle cx="16" cy="16" r="14" stroke="currentColor" strokeWidth="1.6" />
        <path
          d="M16 4.5c2.4 2.2 3.8 5.4 3.8 11.5S18.4 25.3 16 27.5c-2.4-2.2-3.8-5.4-3.8-11.5S13.6 6.7 16 4.5Z"
          stroke="currentColor"
          strokeWidth="1.2"
        />
        <path d="M5.5 16h21" stroke="currentColor" strokeWidth="1.2" />
        <path
          d="M11 12.5 8 16l3 3.5M21 12.5l3 3.5-3 3.5"
          stroke="#c49a3c"
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth="1.8"
        />
      </svg>
    </Box>
  )
}
