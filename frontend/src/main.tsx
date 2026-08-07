import '@fontsource/barlow-condensed/600.css'
import '@fontsource/barlow-condensed/700.css'
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/jetbrains-mono/400.css'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from '@/App'
import { ApplicationProviders } from '@/context/ApplicationProviders'
import { lightTheme } from '@/theme'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ThemeProvider theme={lightTheme}>
      <CssBaseline />
      <ApplicationProviders>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ApplicationProviders>
    </ThemeProvider>
  </StrictMode>,
)
