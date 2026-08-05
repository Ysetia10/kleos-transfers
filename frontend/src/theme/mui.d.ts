import type { CSSProperties } from 'react'
import '@mui/material/Typography'
import '@mui/material/styles'

declare module '@mui/material/styles' {
  interface TypographyVariants {
    display: CSSProperties
    bodyLarge: CSSProperties
  }

  interface TypographyVariantsOptions {
    display?: CSSProperties
    bodyLarge?: CSSProperties
  }
}

declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    display: true
    bodyLarge: true
  }
}
