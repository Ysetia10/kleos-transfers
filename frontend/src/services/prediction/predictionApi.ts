import { httpClient } from '@/services/api/httpClient'
import type { CreatePredictionRequest, Prediction, SpringPage } from '@/types/domain'

export async function createPrediction(request: CreatePredictionRequest): Promise<Prediction> {
  const { data } = await httpClient.post<Prediction>('/api/v1/predictions', request)
  return data
}

export async function listPredictions(page = 0, size = 20): Promise<SpringPage<Prediction>> {
  const { data } = await httpClient.get<SpringPage<Prediction>>('/api/v1/predictions', {
    params: { page, size, sort: 'createdAt,desc' },
  })
  return data
}

export async function getPrediction(id: string): Promise<Prediction> {
  const { data } = await httpClient.get<Prediction>(`/api/v1/predictions/${id}`)
  return data
}
