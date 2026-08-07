import { httpClient } from '@/services/api/httpClient'
import type { Player, SpringPage } from '@/types/domain'

export async function listPlayers(
  page = 0,
  size = 20,
  query?: string,
): Promise<SpringPage<Player>> {
  const { data } = await httpClient.get<SpringPage<Player>>('/api/v1/players', {
    params: {
      page,
      size,
      sort: 'fullName,asc',
      ...(query?.trim() ? { q: query.trim() } : {}),
    },
  })
  return data
}

export async function getPlayer(id: string): Promise<Player> {
  const { data } = await httpClient.get<Player>(`/api/v1/players/${id}`)
  return data
}
