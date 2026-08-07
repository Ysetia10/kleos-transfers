import type { PaletteOptions } from '@mui/material/styles'

export type ApplicationThemeMode = 'light' | 'dark'

export const palettes: Record<ApplicationThemeMode, PaletteOptions> = {
  light: {
    primary: { main: '#163528', light: '#2a5240', dark: '#0e241b', contrastText: '#f7faf7' },
    secondary: { main: '#4a6356', contrastText: '#ffffff' },
    accent: { main: '#c49a3c', light: '#dbb65d', dark: '#8f6e24', contrastText: '#1a1408' },
    success: { main: '#2f6f4e', contrastText: '#ffffff' },
    warning: { main: '#b7791f', contrastText: '#ffffff' },
    error: { main: '#b42318', contrastText: '#ffffff' },
    info: { main: '#2f5d7a', contrastText: '#ffffff' },
    background: { default: '#eef2ee', paper: '#fbfcfb' },
    text: { primary: '#121a15', secondary: '#45544c' },
    divider: '#cfd8d2',
    pitch: {
      line: 'rgba(22, 53, 40, 0.08)',
      deep: '#0f241c',
      mist: 'rgba(196, 154, 60, 0.12)',
    },
  },
  dark: {
    primary: { main: '#9bb8a7', contrastText: '#122018' },
    secondary: { main: '#a7b3ad', contrastText: '#122018' },
    accent: { main: '#e0b35c', contrastText: '#1a1408' },
    success: { main: '#6fbf93', contrastText: '#122018' },
    warning: { main: '#e0b35c', contrastText: '#122018' },
    error: { main: '#f0887e', contrastText: '#122018' },
    info: { main: '#7eafcb', contrastText: '#122018' },
    background: { default: '#121916', paper: '#1c2420' },
    text: { primary: '#f2f5f3', secondary: '#b7c2bc' },
    divider: '#334039',
    pitch: {
      line: 'rgba(155, 184, 167, 0.12)',
      deep: '#0a100e',
      mist: 'rgba(224, 179, 92, 0.1)',
    },
  },
}
