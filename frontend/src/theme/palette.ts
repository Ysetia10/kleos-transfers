import type { PaletteOptions } from '@mui/material/styles'

export type ApplicationThemeMode = 'light' | 'dark'

export const palettes: Record<ApplicationThemeMode, PaletteOptions> = {
  light: {
    primary: { main: '#1f3a2e', contrastText: '#ffffff' },
    secondary: { main: '#5c6b63', contrastText: '#ffffff' },
    success: { main: '#2f6f4e', contrastText: '#ffffff' },
    warning: { main: '#b7791f', contrastText: '#ffffff' },
    error: { main: '#b42318', contrastText: '#ffffff' },
    info: { main: '#2f5d7a', contrastText: '#ffffff' },
    background: { default: '#f6f7f5', paper: '#ffffff' },
    text: { primary: '#172019', secondary: '#4d5a53' },
    divider: '#d8ddd9',
  },
  dark: {
    primary: { main: '#9bb8a7', contrastText: '#122018' },
    secondary: { main: '#a7b3ad', contrastText: '#122018' },
    success: { main: '#6fbf93', contrastText: '#122018' },
    warning: { main: '#e0b35c', contrastText: '#122018' },
    error: { main: '#f0887e', contrastText: '#122018' },
    info: { main: '#7eafcb', contrastText: '#122018' },
    background: { default: '#121916', paper: '#1c2420' },
    text: { primary: '#f2f5f3', secondary: '#b7c2bc' },
    divider: '#334039',
  },
}
