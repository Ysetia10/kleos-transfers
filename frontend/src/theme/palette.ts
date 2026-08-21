import type { PaletteOptions } from '@mui/material/styles'

export type ApplicationThemeMode = 'light' | 'dark'

/**
 * Analytics workspace tokens — blue CTAs, green brand/positive, readable light+dark.
 */
export const palettes: Record<ApplicationThemeMode, PaletteOptions> = {
  light: {
    primary: { main: '#2563EB', light: '#60A5FA', dark: '#1D4ED8', contrastText: '#FFFFFF' },
    secondary: { main: '#64748B', contrastText: '#FFFFFF' },
    accent: { main: '#059669', light: '#34D399', dark: '#047857', contrastText: '#FFFFFF' },
    success: { main: '#059669', contrastText: '#FFFFFF' },
    warning: { main: '#D97706', contrastText: '#FFFFFF' },
    error: { main: '#DC2626', contrastText: '#FFFFFF' },
    info: { main: '#2563EB', contrastText: '#FFFFFF' },
    background: { default: '#FFFFFF', paper: '#FFFFFF' },
    text: { primary: '#0F172A', secondary: '#475569' },
    divider: '#E2E8F0',
    pitch: {
      mist: 'rgba(37, 99, 235, 0.08)',
    },
  },
  dark: {
    primary: { main: '#3B82F6', light: '#60A5FA', dark: '#2563EB', contrastText: '#FFFFFF' },
    secondary: { main: '#94A3B8', contrastText: '#0B0E14' },
    accent: { main: '#22C55E', light: '#4ADE80', dark: '#16A34A', contrastText: '#052E16' },
    success: { main: '#22C55E', contrastText: '#052E16' },
    warning: { main: '#FBBF24', contrastText: '#1C1400' },
    error: { main: '#F87171', contrastText: '#1C0A0A' },
    info: { main: '#60A5FA', contrastText: '#0B0E14' },
    background: { default: '#0B0E14', paper: '#141A22' },
    text: { primary: '#F8FAFC', secondary: '#94A3B8' },
    divider: 'rgba(148, 163, 184, 0.16)',
    pitch: {
      mist: 'rgba(59, 130, 246, 0.12)',
    },
  },
}
