import { httpClient } from '@/services/api/httpClient'
import type { PlayerSeason } from '@/types/domain'

export async function getClubSquad(clubId: string, seasonId: string): Promise<PlayerSeason[]> {
  const { data } = await httpClient.get<PlayerSeason[]>(`/api/v1/clubs/${clubId}/squad`, {
    params: { seasonId },
  })
  return data
}
