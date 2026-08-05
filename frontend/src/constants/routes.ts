export const routes = {
  home: '/',
  prediction: '/prediction',
  players: '/players',
  clubs: '/clubs',
  dashboard: '/dashboard',
  about: '/about',
} as const

export type AppRoute = (typeof routes)[keyof typeof routes]

export const navigationItems: ReadonlyArray<{ label: string; to: AppRoute }> = [
  { label: 'Home', to: routes.home },
  { label: 'Prediction', to: routes.prediction },
  { label: 'Players', to: routes.players },
  { label: 'Clubs', to: routes.clubs },
  { label: 'Dashboard', to: routes.dashboard },
  { label: 'About', to: routes.about },
]
