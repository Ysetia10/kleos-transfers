import { QueryClient } from '@tanstack/react-query'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      // Extra retries in production help survive Render free-tier cold starts.
      retry: import.meta.env.PROD ? 2 : 1,
      retryDelay: (attempt) => Math.min(1_000 * 2 ** attempt, 8_000),
      staleTime: 30_000,
      gcTime: 5 * 60_000,
    },
  },
})
