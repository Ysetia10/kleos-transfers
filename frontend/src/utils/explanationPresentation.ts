import type { Explanation, ExplanationDirection } from '@/types/domain'

export type ExplanationCategoryId =
  | 'availability'
  | 'performance'
  | 'clubFit'
  | 'transfer'

export type CategoryStrength = 'supportive' | 'mixed' | 'headwind'

export interface ExplanationCategoryBundle {
  id: ExplanationCategoryId
  title: string
  signals: Explanation[]
  aggregateImpact: number
  strength: CategoryStrength
  previewSignals: Explanation[]
}

const CATEGORY_ORDER: ExplanationCategoryId[] = [
  'availability',
  'performance',
  'clubFit',
  'transfer',
]

const CATEGORY_TITLES: Record<ExplanationCategoryId, string> = {
  availability: 'Availability & Opportunity',
  performance: 'Player Performance',
  clubFit: 'Club & Tactical Fit',
  transfer: 'Transfer Context',
}

/** Maps backend factor codes to presentation categories (UI-only). */
const FACTOR_CATEGORY: Record<string, ExplanationCategoryId> = {
  INJURY_BURDEN: 'availability',
  SQUAD_VACANCY: 'availability',
  SQUAD_COMPETITION: 'availability',
  MINUTES_INTERVAL: 'availability',
  GK_ROLE: 'availability',
  RECENT_MINUTES: 'performance',
  SCORING_RATE: 'performance',
  CREATION_RATE: 'performance',
  PERFORMANCE_VALUE: 'performance',
  AGE_PROFILE: 'performance',
  ROLE_PRECISION: 'clubFit',
  MANAGER_CONTEXT: 'clubFit',
  LEAGUE_TRANSITION: 'clubFit',
  CONTRACT_PRESSURE: 'transfer',
  DATA_COVERAGE: 'transfer',
}

export function absImpact(item: Explanation): number {
  return Math.abs(Number(item.impact))
}

export function signedImpact(item: Explanation): number {
  const magnitude = absImpact(item)
  if (item.direction === 'NEGATIVE') {
    return -magnitude
  }
  if (item.direction === 'POSITIVE') {
    return magnitude
  }
  return magnitude * 0.25
}

export function categoryForFactor(factorCode: string): ExplanationCategoryId {
  return FACTOR_CATEGORY[factorCode] ?? 'transfer'
}

export function strengthLabel(strength: CategoryStrength): string {
  switch (strength) {
    case 'supportive':
      return 'Supportive'
    case 'headwind':
      return 'Headwind'
    default:
      return 'Mixed'
  }
}

function categoryStrength(aggregate: number): CategoryStrength {
  if (aggregate >= 4) {
    return 'supportive'
  }
  if (aggregate <= -4) {
    return 'headwind'
  }
  return 'mixed'
}

export function getTopDrivers(explanations: Explanation[], limit = 5): Explanation[] {
  return [...explanations]
    .sort((a, b) => absImpact(b) - absImpact(a) || a.sortOrder - b.sortOrder)
    .slice(0, limit)
}

export function getWatchouts(explanations: Explanation[], limit = 4): Explanation[] {
  return [...explanations]
    .filter((item) => item.direction === 'NEUTRAL' || item.direction === 'NEGATIVE')
    .sort((a, b) => absImpact(b) - absImpact(a) || a.sortOrder - b.sortOrder)
    .slice(0, limit)
}

export function buildPredictionSummary(explanations: Explanation[]): string {
  if (explanations.length === 0) {
    return 'Contextual signals from squad, performance, and transfer data shape this estimate.'
  }

  const drivers = getTopDrivers(explanations, 3)
  const supportive = drivers.filter((item) => item.direction === 'POSITIVE')
  const constraints = drivers.filter((item) => item.direction === 'NEGATIVE')
  const uncertain = drivers.filter((item) => item.direction === 'NEUTRAL')

  const joinLabels = (items: Explanation[]) =>
    items.map((item) => item.label.toLowerCase()).join(' and ')

  if (supportive.length > 0 && constraints.length > 0) {
    return `This projection is lifted by ${joinLabels(supportive)}, while ${joinLabels(constraints)} cap the upside.`
  }
  if (supportive.length > 0 && uncertain.length > 0) {
    return `This projection is lifted by ${joinLabels(supportive)}, with ${joinLabels(uncertain)} still an open question.`
  }
  if (supportive.length > 0) {
    return `This projection is mainly supported by ${joinLabels(supportive)}.`
  }
  if (constraints.length > 0) {
    return `This projection is mainly constrained by ${joinLabels(constraints)}.`
  }
  if (uncertain.length > 0) {
    return `This projection hinges on ${joinLabels(uncertain)}, where the model sees limited certainty.`
  }

  return `This projection reflects ${joinLabels(drivers.slice(0, 2))}.`
}

export function groupExplanationsByCategory(
  explanations: Explanation[],
): ExplanationCategoryBundle[] {
  const buckets = new Map<ExplanationCategoryId, Explanation[]>()
  for (const id of CATEGORY_ORDER) {
    buckets.set(id, [])
  }

  for (const item of explanations) {
    const category = categoryForFactor(item.factorCode)
    buckets.get(category)!.push(item)
  }

  return CATEGORY_ORDER.map((id) => {
    const signals = [...(buckets.get(id) ?? [])].sort(
      (a, b) => absImpact(b) - absImpact(a) || a.sortOrder - b.sortOrder,
    )
    const aggregateImpact = signals.reduce((sum, item) => sum + signedImpact(item), 0)
    return {
      id,
      title: CATEGORY_TITLES[id],
      signals,
      aggregateImpact,
      strength: categoryStrength(aggregateImpact),
      previewSignals: signals.slice(0, 5),
    }
  }).filter((bundle) => bundle.signals.length > 0)
}

export function directionTone(
  direction: ExplanationDirection,
): 'success.main' | 'error.main' | 'text.secondary' {
  if (direction === 'POSITIVE') {
    return 'success.main'
  }
  if (direction === 'NEGATIVE') {
    return 'error.main'
  }
  return 'text.secondary'
}

export function driverBarColor(direction: ExplanationDirection): string {
  if (direction === 'POSITIVE') {
    return '#22C55E'
  }
  if (direction === 'NEGATIVE') {
    return '#F87171'
  }
  return '#64748B'
}
