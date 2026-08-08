export type PreferredFoot = 'LEFT' | 'RIGHT' | 'BOTH'
export type Position =
  | 'GK'
  | 'RB'
  | 'CB'
  | 'LB'
  | 'RWB'
  | 'LWB'
  | 'CDM'
  | 'CM'
  | 'CAM'
  | 'RM'
  | 'LM'
  | 'RW'
  | 'LW'
  | 'CF'
  | 'ST'

export type ExplanationDirection = 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL'

export interface Player {
  id: string
  fullName: string
  dateOfBirth: string
  age: number | null
  nationality: string
  heightCm: number | null
  preferredFoot: PreferredFoot | null
  primaryPosition: Position
  fbrefId: string | null
  photoUrl: string | null
  photoAttribution: string | null
  photoLicense: string | null
  photoSource: string | null
  latestClubId: string | null
  latestClubName: string | null
  latestSeasonLabel: string | null
  createdAt: string
  updatedAt: string
}

export interface PlayerSeason {
  id: string
  playerId: string
  playerName: string
  clubId: string
  clubName: string
  seasonId: string
  seasonLabel: string
  appearances: number
  minutesPlayed: number
  goals: number
  assists: number
  xg: number
  xa: number
  primaryPosition: Position
  createdAt: string
  updatedAt: string
}

export interface Club {
  id: string
  name: string
  shortName: string
  countryCode: string
  foundedYear: number | null
  fbrefId: string | null
  crestUrl: string | null
  crestAttribution: string | null
  crestLicense: string | null
  crestSource: string | null
  currentManagerId: string | null
  currentManagerName: string | null
  currentManagerSeasonLabel: string | null
  createdAt: string
  updatedAt: string
}

export interface Season {
  id: string
  label: string
  startDate: string
  endDate: string
  createdAt: string
  updatedAt: string
}

export interface Explanation {
  id: string
  factorCode: string
  label: string
  direction: ExplanationDirection
  impact: number
  detail: string
  sortOrder: number
}

export interface Evaluation {
  id: string
  actualMinutes: number | null
  actualGoals: number | null
  actualAssists: number | null
  actualXg: number | null
  actualXa: number | null
  minutesError: number | null
  goalsError: number | null
  assistsError: number | null
  xgError: number | null
  xaError: number | null
  evaluatedAt: string
}

export interface Prediction {
  id: string
  runId: string
  modelVersion: string
  playerId: string
  playerName: string
  targetClubId: string
  targetClubName: string
  seasonId: string
  seasonLabel: string
  predictedMinutes: number
  predictedGoals: number
  predictedAssists: number
  predictedXg: number
  predictedXa: number
  predictedMarketValueEur: number | null
  compatibilityScore: number
  confidenceScore: number
  explanations: Explanation[]
  evaluation: Evaluation | null
  createdAt: string
  updatedAt: string
}

export interface CreatePredictionRequest {
  playerId: string
  targetClubId: string
  seasonId: string
  note?: string
}

export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
  empty: boolean
}
