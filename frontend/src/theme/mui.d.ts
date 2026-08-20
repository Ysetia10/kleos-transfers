import type { CSSProperties } from 'react'
import '@mui/material/Typography'
import '@mui/material/styles'

declare module '@mui/material/styles' {
  interface Palette {
    accent: Palette['primary']
    pitch: {
      mist: string
    }
  }

  interface PaletteOptions {
    accent?: PaletteOptions['primary']
    pitch?: {
      mist?: string
    }
  }

  interface TypographyVariants {
    bodyLarge: CSSProperties
    normal: CSSProperties
    secondary: CSSProperties
    label: CSSProperties
  }

  interface TypographyVariantsOptions {
    bodyLarge?: CSSProperties
    normal?: CSSProperties
    secondary?: CSSProperties
    label?: CSSProperties
  }
}

declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    bodyLarge: true
    normal: true
    secondary: true
    label: true
  }
}
