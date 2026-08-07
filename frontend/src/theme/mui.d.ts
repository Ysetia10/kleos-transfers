import type { CSSProperties } from 'react'
import '@mui/material/Typography'
import '@mui/material/styles'

declare module '@mui/material/styles' {
  interface Palette {
    accent: Palette['primary']
    pitch: {
      line: string
      deep: string
      mist: string
    }
  }

  interface PaletteOptions {
    accent?: PaletteOptions['primary']
    pitch?: {
      line?: string
      deep?: string
      mist?: string
    }
  }

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

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    accent: true
  }
}
