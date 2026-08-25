import { ApiError } from '@/types/api'

/**
 * Map transport / server failures to calm product copy.
 * Never surface “API”, hosts, stack traces, or raw axios text to the UI.
 */
export function userFacingErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.violations.length > 0) {
      return 'Please check the details below and try again.'
    }
    if (error.status === 404) {
      return "We couldn't find that."
    }
    if (error.status === 409) {
      return 'That conflicts with something already saved.'
    }
    if (error.status === 429) {
      return 'Too many requests. Wait a moment and try again.'
    }
    if (error.status === 400 || error.status === 422) {
      return "We couldn't use those details. Please review and try again."
    }
    if (error.status === 0) {
      return "We couldn't connect right now. The API may be starting up — try again in a moment."
    }
    if (error.status >= 500) {
      return "We couldn't load this right now. Try again in a moment."
    }
    return "We couldn't load this right now. Try again."
  }

  if (error instanceof Error && /timeout|timed out/i.test(error.message)) {
    return 'This is taking longer than usual. Try again in a moment.'
  }

  return "We couldn't load this right now. Try again."
}
