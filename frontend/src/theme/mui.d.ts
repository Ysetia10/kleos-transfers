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
  }

  interface TypographyVariantsOptions {
    bodyLarge?: CSSProperties
  }
}

declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    bodyLarge: true
  }
}
