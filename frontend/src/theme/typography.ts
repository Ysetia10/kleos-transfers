import type { TypographyVariantsOptions } from '@mui/material/styles'

export const fontFamilies = {
  sans: 'Inter, ui-sans-serif, system-ui, sans-serif',
  mono: '"JetBrains Mono", ui-monospace, monospace',
} as const

export const typography: TypographyVariantsOptions = {
  fontFamily: fontFamilies.sans,
  display: {
    fontFamily: fontFamilies.sans,
    fontSize: '3.75rem',
    fontWeight: 600,
    lineHeight: 1.1,
    letterSpacing: '-0.03em',
  },
  h1: {
    fontFamily: fontFamilies.sans,
    fontSize: '2.5rem',
    fontWeight: 600,
    lineHeight: 1.2,
    letterSpacing: '-0.02em',
  },
  h2: {
    fontFamily: fontFamilies.sans,
    fontSize: '2rem',
    fontWeight: 600,
    lineHeight: 1.25,
    letterSpacing: '-0.02em',
  },
  h3: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.5rem',
    fontWeight: 600,
    lineHeight: 1.3,
  },
  h4: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.25rem',
    fontWeight: 600,
    lineHeight: 1.35,
  },
  bodyLarge: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.125rem',
    fontWeight: 400,
    lineHeight: 1.6,
  },
  body1: {
    fontFamily: fontFamilies.sans,
    fontSize: '1rem',
    fontWeight: 400,
    lineHeight: 1.5,
  },
  body2: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: 400,
    lineHeight: 1.45,
  },
  caption: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.75rem',
    fontWeight: 400,
    lineHeight: 1.4,
  },
  button: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: 500,
    lineHeight: 1.2,
    letterSpacing: '0.01em',
    textTransform: 'none',
  },
}
