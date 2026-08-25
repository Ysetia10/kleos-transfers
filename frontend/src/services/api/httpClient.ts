import axios, { AxiosError, type InternalAxiosRequestConfig, isAxiosError } from 'axios'
import { env } from '@/config/env'
import { setApiWaking } from '@/services/api/apiWake'
import { ApiError, type ApiErrorBody } from '@/types/api'

/** Render free tier cold start can exceed 30s; give production more headroom. */
const REQUEST_TIMEOUT_MS = import.meta.env.PROD ? 60_000 : 10_000
const MAX_NETWORK_RETRIES = import.meta.env.PROD ? 2 : 0
const WAKE_BANNER_AFTER_MS = 2_500

type RetryConfig = InternalAxiosRequestConfig & { __retryCount?: number }

const wakeTimers = new WeakMap<object, number>()

export const httpClient = axios.create({
  baseURL: env.apiBaseUrl,
  headers: {
    Accept: 'application/json',
  },
  timeout: REQUEST_TIMEOUT_MS,
})

httpClient.interceptors.request.use((config) => {
  const wakeTimer = window.setTimeout(() => setApiWaking(true), WAKE_BANNER_AFTER_MS)
  wakeTimers.set(config, wakeTimer)
  return config
})

httpClient.interceptors.response.use(
  (response) => {
    clearWakeTimer(response.config)
    setApiWaking(false)
    return response
  },
  async (error: unknown) => {
    if (isAxiosError(error)) {
      clearWakeTimer(error.config)
    }

    if (isAxiosError(error) && shouldRetry(error)) {
      const config = error.config as RetryConfig | undefined
      if (config) {
        const retryCount = config.__retryCount ?? 0
        if (retryCount < MAX_NETWORK_RETRIES) {
          config.__retryCount = retryCount + 1
          setApiWaking(true)
          await sleep(1_000 * 2 ** retryCount)
          return httpClient.request(config)
        }
      }
    }

    setApiWaking(false)
    return Promise.reject(toApiError(error))
  },
)

function clearWakeTimer(config: object | undefined): void {
  if (!config) {
    return
  }
  const wakeTimer = wakeTimers.get(config)
  if (wakeTimer != null) {
    window.clearTimeout(wakeTimer)
    wakeTimers.delete(config)
  }
}

function shouldRetry(error: AxiosError): boolean {
  if (error.response?.status === 429) {
    return false
  }
  if (error.code === 'ECONNABORTED') {
    return true
  }
  return !error.response
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms)
  })
}

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
    return new ApiError('Network unavailable', status)
  }

  return new ApiError(axiosError.message || 'Request failed', status)
}
