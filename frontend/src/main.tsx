import { CssBaseline } from '@mui/material'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from '@/App'
import { ApplicationProviders } from '@/context/ApplicationProviders'
import { ColorModeProvider } from '@/context/ColorModeContext'
import { setApiWaking } from '@/services/api/apiWake'
import { httpClient } from '@/services/api/httpClient'

if (import.meta.env.PROD) {
  void (async () => {
    try {
      setApiWaking(true)
      await httpClient.get('/api/v1/health')
    } catch {
      // Query retries / banner handle failure; prefetch is best-effort.
    } finally {
      setApiWaking(false)
    }
  })()
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ColorModeProvider>
      <CssBaseline />
      <ApplicationProviders>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </ApplicationProviders>
    </ColorModeProvider>
  </StrictMode>,
)
