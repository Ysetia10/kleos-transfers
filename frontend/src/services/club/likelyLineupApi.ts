import { httpClient } from '@/services/api/httpClient'
import type { PlayerSeason } from '@/types/domain'

export interface LikelyLineupPlacement {
  slotId: string
  x: number
  y: number
  player: PlayerSeason
  likelyStarter: boolean
}

export interface LikelyLineup {
  formation: string | null
  rolePrecisionAvailable: boolean
  placements: LikelyLineupPlacement[]
}

export async function getLikelyLineup(clubId: string, seasonId: string): Promise<LikelyLineup> {
  const { data } = await httpClient.get<LikelyLineup>(`/api/v1/clubs/${clubId}/likely-xi`, {
    params: { seasonId },
  })
  return data
}
