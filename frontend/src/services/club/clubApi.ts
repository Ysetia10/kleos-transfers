import { httpClient } from '@/services/api/httpClient'
import type { Club, SpringPage } from '@/types/domain'

export async function listClubs(page = 0, size = 20, query?: string): Promise<SpringPage<Club>> {
  const { data } = await httpClient.get<SpringPage<Club>>('/api/v1/clubs', {
    params: {
      page,
      size,
      sort: 'name,asc',
      ...(query?.trim() ? { q: query.trim() } : {}),
    },
  })
  return data
}

export async function getClub(id: string): Promise<Club> {
  const { data } = await httpClient.get<Club>(`/api/v1/clubs/${id}`)
  return data
}
