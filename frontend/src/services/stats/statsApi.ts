import { httpClient } from '@/services/api/httpClient'

export type LeagueCode = 'PREMIER_LEAGUE' | 'LA_LIGA'

export interface LeaderboardEntry {
  playerId: string
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
