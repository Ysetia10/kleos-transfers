export type PreferredFoot = 'LEFT' | 'RIGHT' | 'BOTH'
export type DateOfBirthPrecision = 'DAY' | 'YEAR'
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

export interface TransferMoveSummary {
  transferId: string
  fromClubId: string | null
  fromClubName: string | null
  toClubId: string | null
  toClubName: string | null
  feeEur: number | null
  transferDate: string | null
  seasonLabel: string | null
  seasonStartDate: string | null
}

export interface Player {
  id: string
  fullName: string
  dateOfBirth: string
  dateOfBirthPrecision: DateOfBirthPrecision
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
  latestTransfer: TransferMoveSummary | null
  createdAt: string
  updatedAt: string
}

export interface PlayerSeason {
  id: string
  playerId: string
  playerName: string
  photoUrl: string | null
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
  inboundTransfer: TransferMoveSummary | null
}

export type TacticalSystem = 'POSSESSION' | 'TRANSITION' | 'DIRECT' | 'BALANCED'
export type TempoProfile = 'LOW' | 'MEDIUM' | 'HIGH'
export type RecruitmentSignal = 'HIGH' | 'MEDIUM' | 'LOW' | 'UNKNOWN'

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
  currentManagerFirstSeasonAtClub: boolean | null
  tacticalSystem: TacticalSystem | null
  tempo: TempoProfile | null
  youthMinutesPct: number | null
  fitIndex: number
  recruitmentSignal: RecruitmentSignal
  fitIndexVersion: string
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
  minutesError: number | null
  goalsError: number | null
  assistsError: number | null
  evaluatedAt: string
}

export interface CompatibilityBreakdown {
  system: number
  role: number
  tempo: number
  league: number
  manager: number
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
  predictedMinutesLow: number
  predictedMinutesHigh: number
  predictedGoals: number
  predictedAssists: number
  predictedMarketValueEur: number | null
  compatibilityScore: number
  compatibilityBreakdown: CompatibilityBreakdown | null
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
