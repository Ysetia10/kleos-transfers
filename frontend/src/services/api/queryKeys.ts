export const queryKeys = {
  health: ['health'] as const,
  players: {
    all: ['players'] as const,
    list: (page: number, size: number) => ['players', 'list', page, size] as const,
    detail: (id: string) => ['players', 'detail', id] as const,
  },
  clubs: {
    all: ['clubs'] as const,
    detail: (id: string) => ['clubs', 'detail', id] as const,
  },
} as const
