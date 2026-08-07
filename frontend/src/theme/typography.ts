import type { TypographyVariantsOptions } from '@mui/material/styles'

export const fontFamilies = {
  display: '"Barlow Condensed", "Arial Narrow", sans-serif',
  sans: 'Inter, ui-sans-serif, system-ui, sans-serif',
  mono: '"JetBrains Mono", ui-monospace, monospace',
} as const

export const typography: TypographyVariantsOptions = {
  fontFamily: fontFamilies.sans,
  display: {
    fontFamily: fontFamilies.display,
    fontSize: '4rem',
    fontWeight: 700,
    lineHeight: 0.95,
    letterSpacing: '0.02em',
    textTransform: 'uppercase',
  },
  h1: {
    fontFamily: fontFamilies.display,
    fontSize: '2.75rem',
    fontWeight: 700,
    lineHeight: 1.05,
    letterSpacing: '0.02em',
    textTransform: 'uppercase',
  },
  h2: {
    fontFamily: fontFamilies.display,
    fontSize: '2.15rem',
    fontWeight: 700,
    lineHeight: 1.1,
    letterSpacing: '0.02em',
    textTransform: 'uppercase',
  },
  h3: {
    fontFamily: fontFamilies.display,
    fontSize: '1.55rem',
    fontWeight: 600,
    lineHeight: 1.2,
    letterSpacing: '0.03em',
    textTransform: 'uppercase',
  },
  h4: {
    fontFamily: fontFamilies.display,
    fontSize: '1.25rem',
    fontWeight: 600,
    lineHeight: 1.25,
    letterSpacing: '0.04em',
    textTransform: 'uppercase',
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
    lineHeight: 1.55,
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
    fontWeight: 600,
    lineHeight: 1.2,
    letterSpacing: '0.02em',
    textTransform: 'none',
  },
}
