import { ThemeProvider } from '@mui/material/styles'
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren,
} from 'react'
import { createKleosTheme } from '@/theme'
import type { ApplicationThemeMode } from '@/theme/palette'

const STORAGE_KEY = 'kleos-color-mode'

interface ColorModeContextValue {
  mode: ApplicationThemeMode
  toggleMode: () => void
  setMode: (mode: ApplicationThemeMode) => void
}

const ColorModeContext = createContext<ColorModeContextValue | null>(null)

function readStoredMode(): ApplicationThemeMode {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark') {
      return stored
    }
  } catch {
    // ignore
  }
  if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: light)').matches) {
    return 'light'
  }
  return 'dark'
}

export function ColorModeProvider({ children }: PropsWithChildren) {
  const [mode, setModeState] = useState<ApplicationThemeMode>(() => readStoredMode())

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, mode)
    } catch {
      // ignore
    }
    document.documentElement.dataset.colorMode = mode
  }, [mode])

  const setMode = useCallback((next: ApplicationThemeMode) => {
    setModeState(next)
  }, [])

  const toggleMode = useCallback(() => {
    setModeState((current) => (current === 'dark' ? 'light' : 'dark'))
  }, [])

  const theme = useMemo(() => createKleosTheme(mode), [mode])

  const value = useMemo(
    () => ({ mode, toggleMode, setMode }),
    [mode, toggleMode, setMode],
  )

  return (
    <ColorModeContext.Provider value={value}>
      <ThemeProvider theme={theme}>{children}</ThemeProvider>
    </ColorModeContext.Provider>
  )
}

export function useColorMode() {
  const ctx = useContext(ColorModeContext)
  if (!ctx) {
    throw new Error('useColorMode must be used within ColorModeProvider')
  }
  return ctx
}
