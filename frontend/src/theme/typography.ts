import type { TypographyVariantsOptions } from '@mui/material/styles'

export const fontFamilies = {
  sans: 'Inter, ui-sans-serif, system-ui, sans-serif',
  mono: '"JetBrains Mono", ui-monospace, monospace',
} as const

export const typography: TypographyVariantsOptions = {
  fontFamily: fontFamilies.sans,
  h1: {
    fontFamily: fontFamilies.sans,
    fontSize: '2.25rem',
    fontWeight: 700,
    lineHeight: 1.15,
    letterSpacing: '-0.02em',
  },
  h2: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.75rem',
    fontWeight: 700,
    lineHeight: 1.2,
    letterSpacing: '-0.02em',
  },
  h3: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.25rem',
    fontWeight: 600,
    lineHeight: 1.25,
    letterSpacing: '-0.01em',
  },
  h4: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.05rem',
    fontWeight: 600,
    lineHeight: 1.3,
  },
  bodyLarge: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.0625rem',
    fontWeight: 400,
    lineHeight: 1.6,
  },
  body1: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.95rem',
    fontWeight: 400,
    lineHeight: 1.55,
  },
  body2: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: 400,
    lineHeight: 1.45,
  },
  caption: {
    fontFamily: fontFamilies.mono,
    fontSize: '0.7rem',
    fontWeight: 500,
    lineHeight: 1.4,
    letterSpacing: '0.06em',
    textTransform: 'uppercase',
  },
  button: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: 600,
    lineHeight: 1.2,
    letterSpacing: '0.01em',
    textTransform: 'none',
  },
  subtitle2: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: 600,
    lineHeight: 1.35,
  },
}
