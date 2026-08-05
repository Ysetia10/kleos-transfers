function readApiBaseUrl(): string | undefined {
  const value = import.meta.env.VITE_API_BASE_URL?.trim()
  return value ? value.replace(/\/$/, '') : undefined
}

export const env = {
  apiBaseUrl: readApiBaseUrl(),
} as const
