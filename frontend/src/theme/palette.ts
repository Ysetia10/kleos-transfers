import type { PaletteOptions } from '@mui/material/styles'

export type ApplicationThemeMode = 'light' | 'dark'

export const palettes: Record<ApplicationThemeMode, PaletteOptions> = {
  light: {
    primary: { main: '#404040', contrastText: '#ffffff' },
    secondary: { main: '#737373', contrastText: '#ffffff' },
    success: { main: '#525252', contrastText: '#ffffff' },
    warning: { main: '#666666', contrastText: '#ffffff' },
    error: { main: '#333333', contrastText: '#ffffff' },
    info: { main: '#595959', contrastText: '#ffffff' },
    background: { default: '#fafafa', paper: '#ffffff' },
    text: { primary: '#171717', secondary: '#525252' },
    divider: '#e5e5e5',
  },
  dark: {
    primary: { main: '#d4d4d4', contrastText: '#171717' },
    secondary: { main: '#a3a3a3', contrastText: '#171717' },
    success: { main: '#b3b3b3', contrastText: '#171717' },
    warning: { main: '#c4c4c4', contrastText: '#171717' },
    error: { main: '#e0e0e0', contrastText: '#171717' },
    info: { main: '#bdbdbd', contrastText: '#171717' },
    background: { default: '#171717', paper: '#262626' },
    text: { primary: '#fafafa', secondary: '#c4c4c4' },
    divider: '#404040',
  },
}
