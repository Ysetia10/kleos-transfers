import { createTheme } from '@mui/material/styles'
import { breakpoints } from './breakpoints'
import { components } from './components'
import { palettes, type ApplicationThemeMode } from './palette'
import { shape } from './shape'
import { shadows } from './shadows'
import { spacing } from './spacing'
import { typography } from './typography'

export function createKleosTheme(mode: ApplicationThemeMode) {
  return createTheme({
    palette: {
      mode,
      ...palettes[mode],
    },
    typography,
    spacing,
    breakpoints,
    shadows,
    shape,
    components,
  })
}

export const lightTheme = createKleosTheme('light')
export const darkTheme = createKleosTheme('dark')
