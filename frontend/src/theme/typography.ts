import type { TypographyVariantsOptions } from '@mui/material/styles'

/**
 * Single product typeface: Arial throughout.
 * No bold (600/700): emphasis uses medium weight 500.
 *
 * Content roles:
 * - normal (`body1` / `body2` / `normal`): primary text and numbers — 400
 * - secondary (`secondary`): units / metadata — 400; use with color="text.secondary"
 * - label (`label`): column headers — 500
 */
export const fontFamilies = {
  sans: 'Arial, Helvetica, sans-serif',
  mono: 'Arial, Helvetica, sans-serif',
} as const

const medium = 500 as const

export const typography: TypographyVariantsOptions = {
  fontFamily: fontFamilies.sans,
  fontWeightBold: medium,
  fontWeightMedium: medium,
  h1: {
    fontFamily: fontFamilies.sans,
    fontSize: '2.25rem',
    fontWeight: medium,
    lineHeight: 1.15,
    letterSpacing: '-0.01em',
  },
  h2: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.75rem',
    fontWeight: medium,
    lineHeight: 1.2,
    letterSpacing: '-0.01em',
  },
  h3: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.25rem',
    fontWeight: medium,
    lineHeight: 1.25,
  },
  h4: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.05rem',
    fontWeight: medium,
    lineHeight: 1.3,
  },
  h5: {
    fontFamily: fontFamilies.sans,
    fontSize: '1.15rem',
    fontWeight: medium,
    lineHeight: 1.3,
  },
  h6: {
    fontFamily: fontFamilies.sans,
    fontSize: '1rem',
    fontWeight: medium,
    lineHeight: 1.35,
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
  normal: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: 400,
    lineHeight: 1.45,
  },
  secondary: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.8125rem',
    fontWeight: 400,
    lineHeight: 1.4,
  },
  label: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.75rem',
    fontWeight: medium,
    lineHeight: 1.4,
    letterSpacing: '0.02em',
  },
  caption: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.7rem',
    fontWeight: medium,
    lineHeight: 1.4,
    letterSpacing: '0.04em',
    textTransform: 'uppercase',
  },
  button: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: medium,
    lineHeight: 1.2,
    letterSpacing: '0.01em',
    textTransform: 'none',
  },
  subtitle1: {
    fontFamily: fontFamilies.sans,
    fontSize: '1rem',
    fontWeight: medium,
    lineHeight: 1.4,
  },
  subtitle2: {
    fontFamily: fontFamilies.sans,
    fontSize: '0.875rem',
    fontWeight: medium,
    lineHeight: 1.35,
  },
}
