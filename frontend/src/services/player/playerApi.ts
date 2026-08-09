import { httpClient } from '@/services/api/httpClient'
import type { Player, SpringPage } from '@/types/domain'

export interface PlayerListFilters {
  query?: string
  position?: string
  league?: string
  minAge?: number
  maxAge?: number
}

export async function listPlayers(
  page = 0,
  size = 20,
  filters: PlayerListFilters | string = {},
): Promise<SpringPage<Player>> {
  const normalized: PlayerListFilters =
    typeof filters === 'string' ? { query: filters } : (filters ?? {})

  const { data } = await httpClient.get<SpringPage<Player>>('/api/v1/players', {
    params: {
      page,
      size,
      sort: 'fullName,asc',
      ...(normalized.query?.trim() ? { q: normalized.query.trim() } : {}),
      ...(normalized.position?.trim() ? { position: normalized.position.trim() } : {}),
      ...(normalized.league?.trim() ? { league: normalized.league.trim() } : {}),
      ...(normalized.minAge != null ? { minAge: normalized.minAge } : {}),
      ...(normalized.maxAge != null ? { maxAge: normalized.maxAge } : {}),
    },
  })
  return data
}

export async function getPlayer(id: string): Promise<Player> {
  const { data } = await httpClient.get<Player>(`/api/v1/players/${id}`)
  return data
}
