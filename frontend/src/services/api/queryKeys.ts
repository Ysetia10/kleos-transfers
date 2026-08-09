export const queryKeys = {
  players: {
    list: (
      page: number,
      size: number,
      filters:
        | string
        | {
            query?: string
            position?: string
            league?: string
            minAge?: number
            maxAge?: number
          } = '',
    ) => ['players', 'list', page, size, filters] as const,
    detail: (id: string) => ['players', 'detail', id] as const,
  },
  clubs: {
    list: (page: number, size: number, query = '') =>
      ['clubs', 'list', page, size, query] as const,
    detail: (id: string) => ['clubs', 'detail', id] as const,
    squad: (clubId: string, seasonId: string) => ['clubs', 'squad', clubId, seasonId] as const,
  },
  seasons: {
    list: (page: number, size: number) => ['seasons', 'list', page, size] as const,
  },
  predictions: {
    list: (page: number, size: number) => ['predictions', 'list', page, size] as const,
    detail: (id: string) => ['predictions', 'detail', id] as const,
  },
  stats: {
    trending: (seasonId = '', limit = 3) => ['stats', 'trending', seasonId, limit] as const,
    allTime: (limit = 10) => ['stats', 'all-time', limit] as const,
  },
} as const
