import { httpClient } from '@/services/api/httpClient'
import type { Injury, SpringPage } from '@/types/domain'

export async function listInjuries(
  page = 0,
  size = 20,
  options?: { playerId?: string },
): Promise<SpringPage<Injury>> {
  const { data } = await httpClient.get<SpringPage<Injury>>('/api/v1/injuries', {
    params: {
      page,
      size,
      ...(options?.playerId ? { playerId: options.playerId } : {}),
    },
  })
  return data
}
