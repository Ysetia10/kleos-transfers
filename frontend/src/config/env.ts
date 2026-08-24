/**
 * API base URL — never hardcode localhost here.
 *
 * Local: set VITE_API_BASE_URL in frontend/.env.local (see .env.example).
 * Vercel: set VITE_API_BASE_URL to your Render HTTPS origin at build time.
 */
function readApiBaseUrl(): string | undefined {
  const value = import.meta.env.VITE_API_BASE_URL?.trim()
  return value ? value.replace(/\/$/, '') : undefined
}

const apiBaseUrl = readApiBaseUrl()

if (import.meta.env.PROD && !apiBaseUrl) {
  console.error(
    'VITE_API_BASE_URL is not set. Configure it in Vercel (Production) before deploying.',
  )
}

export const env = {
  apiBaseUrl,
  isApiConfigured: Boolean(apiBaseUrl),
} as const
