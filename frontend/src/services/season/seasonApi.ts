import { httpClient } from '@/services/api/httpClient'
import type { Season, SpringPage } from '@/types/domain'

export async function listSeasons(page = 0, size = 50): Promise<SpringPage<Season>> {
  const { data } = await httpClient.get<SpringPage<Season>>('/api/v1/seasons', {
    params: { page, size, sort: 'startDate,desc' },
  })
  return data
}
