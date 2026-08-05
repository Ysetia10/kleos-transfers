import axios, { AxiosError, isAxiosError } from 'axios'
import { env } from '@/config/env'
import { ApiError, type ApiErrorBody } from '@/types/api'

export const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    Accept: 'application/json',
  },
  timeout: 10_000,
})

httpClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => Promise.reject(toApiError(error)),
)

function toApiError(error: unknown): ApiError {
  if (!isAxiosError(error)) {
    return new ApiError('Unexpected client error', 0)
  }

  const axiosError = error as AxiosError<ApiErrorBody>
  const body = axiosError.response?.data
  const status = axiosError.response?.status ?? 0

  if (body && typeof body === 'object' && typeof body.message === 'string') {
    return new ApiError(body.message, body.status ?? status, body.path, body.violations ?? [])
  }

  if (axiosError.code === 'ECONNABORTED') {
    return new ApiError('Request timed out', status)
  }

  if (!axiosError.response) {
    return new ApiError('Unable to reach the API', status)
  }

  return new ApiError(axiosError.message || 'Request failed', status)
}
