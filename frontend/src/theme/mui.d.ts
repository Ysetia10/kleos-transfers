import '@mui/material/Typography'
import '@mui/material/styles'

declare module '@mui/material/styles' {
  interface TypographyVariants {
    display: React.CSSProperties
    bodyLarge: React.CSSProperties
  }

  interface TypographyVariantsOptions {
    display?: React.CSSProperties
    bodyLarge?: React.CSSProperties
  }
}

declare module '@mui/material/Typography' {
  interface TypographyPropsVariantOverrides {
    display: true
    bodyLarge: true
  }
}
