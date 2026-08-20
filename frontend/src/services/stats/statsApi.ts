import { httpClient } from '@/services/api/httpClient'

export type LeagueCode =
  | 'PREMIER_LEAGUE'
  | 'LA_LIGA'
  | 'BUNDESLIGA'
  | 'SERIE_A'
  | 'LIGUE_1'

export interface LeaderboardEntry {
  playerId: string | null
  playerName: string
  clubId: string | null
  clubName: string | null
  goals: number
  assists: number
  appearances: number
  minutesPlayed: number
  seasonsPlayed: number
}

export interface LeagueBoards {
  league: LeagueCode
  tournamentName: string
  seasonId: string | null
  seasonLabel: string | null
  coverageNote: string | null
  topScorers: LeaderboardEntry[]
  topAssisters: LeaderboardEntry[]
}

export async function fetchTrendingStats(seasonId?: string, limit = 3): Promise<LeagueBoards[]> {
  const { data } = await httpClient.get<LeagueBoards[]>('/api/v1/stats/trending', {
    params: {
      limit,
      ...(seasonId ? { seasonId } : {}),
    },
  })
  return data
}

export async function fetchAllTimeStats(limit = 10): Promise<LeagueBoards[]> {
  const { data } = await httpClient.get<LeagueBoards[]>('/api/v1/stats/all-time', {
    params: { limit },
  })
  return data
}

export interface FitRoute {
  playerId: string
  playerName: string
  playerPhotoUrl: string | null
  fromClubId: string | null
  fromClubName: string | null
  toClubId: string
  toClubName: string
  seasonId: string
  seasonLabel: string
  compatibilityScore: number
  predictedMinutes: number
  predictionId: string | null
  source: 'STORED_PREDICTION' | 'HYPOTHETICAL' | string
}

export async function fetchHighestFitRoutes(limit = 8): Promise<FitRoute[]> {
  const { data } = await httpClient.get<FitRoute[]>('/api/v1/stats/fit-routes', {
    params: { limit },
  })
  return data
}

export interface MetricTriple {
  mae: number
  rmse: number
  bias_actual_minus_predicted: number
}

export interface MetricBlock {
  n: number
  minutes?: MetricTriple
  goals?: MetricTriple
  assists?: MetricTriple
}

export interface AccuracySample {
  player: string
  playerId?: string
  position?: string
  club: string
  clubId?: string
  season: string
  league?: string
  countryCode?: string
  predictedMinutes: number
  actualMinutes: number
  minutesError?: number
  predictedGoals: number
  actualGoals: number
  goalsError?: number
  predictedAssists: number
  actualAssists: number
  assistsError?: number
  predictionId?: string
}

export interface LeagueAccuracy {
  countryCode: string
  leagueName: string
  metrics: MetricBlock
  samples: AccuracySample[]
}

export interface ModelAccuracy {
  generatedAt: string
  seasons: string[]
  modelVersion: string
  metrics: MetricBlock
  byLeague: Record<string, LeagueAccuracy>
  samplePredictions: AccuracySample[]
}

export async function fetchModelAccuracy(): Promise<ModelAccuracy> {
  const { data } = await httpClient.get<ModelAccuracy>('/api/v1/stats/model-accuracy')
  return data
}
