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

export type AppRoute =
  | (typeof routes)[Exclude<keyof typeof routes, 'predictionDetail' | 'playerDetail' | 'clubDetail'>]
  | ReturnType<typeof routes.predictionDetail>
  | ReturnType<typeof routes.playerDetail>
  | ReturnType<typeof routes.clubDetail>

export const navigationItems: ReadonlyArray<{ label: string; to: AppRoute }> = [
  { label: 'Home', to: routes.home },
  { label: 'Prediction', to: routes.prediction },
  { label: 'Players', to: routes.players },
  { label: 'Clubs', to: routes.clubs },
  { label: 'Dashboard', to: routes.dashboard },
  { label: 'About', to: routes.about },
]
