export const queryKeys = {
  health: ['health'] as const,
  players: {
    all: ['players'] as const,
    list: (page: number, size: number, query = '') =>
      ['players', 'list', page, size, query] as const,
    detail: (id: string) => ['players', 'detail', id] as const,
  },
  clubs: {
    all: ['clubs'] as const,
    list: (page: number, size: number, query = '') =>
      ['clubs', 'list', page, size, query] as const,
    detail: (id: string) => ['clubs', 'detail', id] as const,
  },
  seasons: {
    all: ['seasons'] as const,
    list: (page: number, size: number) => ['seasons', 'list', page, size] as const,
  },
  predictions: {
    all: ['predictions'] as const,
    list: (page: number, size: number) => ['predictions', 'list', page, size] as const,
    detail: (id: string) => ['predictions', 'detail', id] as const,
  },
} as const
