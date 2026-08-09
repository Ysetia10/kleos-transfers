export const routes = {
  home: '/',
  prediction: '/prediction',
  predictionDetail: (id: string) => `/predictions/${id}` as const,
  players: '/players',
  playerDetail: (id: string) => `/players/${id}` as const,
  clubs: '/clubs',
  clubDetail: (id: string) => `/clubs/${id}` as const,
  transfers: '/transfers',
  trending: '/trending',
  methodology: '/methodology',
  dashboard: '/dashboard',
  about: '/about',
} as const

export type AppRoute =
  | (typeof routes)[Exclude<
      keyof typeof routes,
      'predictionDetail' | 'playerDetail' | 'clubDetail'
    >]
  | ReturnType<typeof routes.predictionDetail>
  | ReturnType<typeof routes.playerDetail>
  | ReturnType<typeof routes.clubDetail>

/** Deep-link into the simulator with optional pre-selected identities. */
export function homePredictPath(options?: { playerId?: string; clubId?: string }) {
  const params = new URLSearchParams()
  if (options?.playerId) {
    params.set('playerId', options.playerId)
  }
  if (options?.clubId) {
    params.set('clubId', options.clubId)
  }
  const search = params.toString()
  return `${routes.home}${search ? `?${search}` : ''}`
}

export const navigationItems: ReadonlyArray<{ label: string; to: AppRoute }> = [
  { label: 'Prediction', to: routes.home },
  { label: 'Players', to: routes.players },
  { label: 'Clubs', to: routes.clubs },
  { label: 'Transfers', to: routes.transfers },
  { label: 'Trending', to: routes.trending },
  { label: 'Methodology', to: routes.methodology },
]
