import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
// 700 weight used for brand wordmark / page titles
import '@fontsource/jetbrains-mono/400.css'
import { CssBaseline } from '@mui/material'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from '@/App'
import { ApplicationProviders } from '@/context/ApplicationProviders'
import { ColorModeProvider } from '@/context/ColorModeContext'

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
