export interface ApiFieldViolation {
  field: string
  message: string
}

export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  violations: ApiFieldViolation[]
}

export class ApiError extends Error {
  readonly status: number
  readonly path?: string
  readonly violations: ApiFieldViolation[]

  constructor(message: string, status: number, path?: string, violations: ApiFieldViolation[] = []) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.path = path
    this.violations = violations
  }
}
