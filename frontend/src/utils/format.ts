export function formatNumber(value: number, fractionDigits = 0): string {
  return new Intl.NumberFormat('en-GB', {
    maximumFractionDigits: fractionDigits,
    minimumFractionDigits: fractionDigits,
  }).format(value)
}

export function formatEur(value: number | null | undefined): string {
  if (value == null) {
    return '—'
  }
  return new Intl.NumberFormat('en-GB', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatDate(value: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value))
}

/** True when the stored date is the FBref mid-year age anchor (1 July). */
export function isYearOnlyBirthDate(
  value: string,
  precision: 'DAY' | 'YEAR' | null | undefined,
): boolean {
  if (precision === 'YEAR') {
    return true
  }
  // Defensive: older clients / missing precision must not show fake 1 July.
  if (precision == null && /^\d{4}-07-01$/.test(value)) {
    return true
  }
  return false
}

/** Full date when known; year only when we lack a verified day-level DOB. */
export function formatDateOfBirth(
  value: string,
  precision: 'DAY' | 'YEAR' | null | undefined,
): string {
  if (isYearOnlyBirthDate(value, precision)) {
    const year = Number.parseInt(value.slice(0, 4), 10)
    return Number.isFinite(year) ? String(year) : value.slice(0, 4)
  }
  return formatDate(value)
}

export function formatAge(age: number | null | undefined): string {
  if (age == null) {
    return '—'
  }
  return String(age)
}

export function formatDateTime(value: string): string {
  return new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
