export const routes = {
  home: '/',
  prediction: '/prediction',
  predictionDetail: (id: string) => `/predictions/${id}` as const,
  players: '/players',
  playerDetail: (id: string) => `/players/${id}` as const,
  clubs: '/clubs',
  clubDetail: (id: string) => `/clubs/${id}` as const,
  dashboard: '/dashboard',
  about: '/about',
} as const

export const homeSections = {
  predict: 'predict',
  recent: 'recent',
  trending: 'trending',
  allTime: 'all-time',
  catalogue: 'catalogue',
} as const

export type HomeSectionId = (typeof homeSections)[keyof typeof homeSections]

export type AppRoute =
  | (typeof routes)[Exclude<keyof typeof routes, 'predictionDetail' | 'playerDetail' | 'clubDetail'>]
  | ReturnType<typeof routes.predictionDetail>
  | ReturnType<typeof routes.playerDetail>
  | ReturnType<typeof routes.clubDetail>

/** Deep-link into the Home prediction section, optionally pre-selecting identities. */
export function homePredictPath(options?: { playerId?: string; clubId?: string }) {
  const params = new URLSearchParams()
  if (options?.playerId) {
    params.set('playerId', options.playerId)
  }
  if (options?.clubId) {
    params.set('clubId', options.clubId)
  }
  const search = params.toString()
  return `${routes.home}${search ? `?${search}` : ''}#${homeSections.predict}`
}

/** Primary chrome links — workspace lives on Home; avoid a crowded top bar. */
export const navigationItems: ReadonlyArray<{ label: string; to: AppRoute }> = [
  { label: 'About', to: routes.about },
]

export const homeJumpLinks: ReadonlyArray<{ label: string; section: HomeSectionId }> = [
  { label: 'Simulator', section: homeSections.predict },
  { label: 'Trending', section: homeSections.trending },
  { label: 'All-time', section: homeSections.allTime },
  { label: 'Recent', section: homeSections.recent },
  { label: 'Players & clubs', section: homeSections.catalogue },
]
