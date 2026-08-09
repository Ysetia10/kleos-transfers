import type { PlayerSeason, Position } from '@/types/domain'

export type FormationId = '4-3-3' | '4-2-3-1'

export type PitchSlotId =
  | 'GK'
  | 'LB'
  | 'LCB'
  | 'RCB'
  | 'RB'
  | 'LDM'
  | 'CDM'
  | 'RDM'
  | 'LCM'
  | 'CM'
  | 'RCM'
  | 'LAM'
  | 'CAM'
  | 'RAM'
  | 'LW'
  | 'ST'
  | 'RW'

export interface PitchSlot {
  id: PitchSlotId
  /** 0 = goal line, 1 = opposition box */
  y: number
  /** 0 = left touchline, 1 = right */
  x: number
}

export interface PitchPlacement {
  slot: PitchSlot
  player: PlayerSeason
}

const FORMATION_433: PitchSlot[] = [
  { id: 'GK', x: 0.5, y: 0.08 },
  { id: 'LB', x: 0.12, y: 0.28 },
  { id: 'LCB', x: 0.36, y: 0.26 },
  { id: 'RCB', x: 0.64, y: 0.26 },
  { id: 'RB', x: 0.88, y: 0.28 },
  { id: 'LCM', x: 0.28, y: 0.5 },
  { id: 'CM', x: 0.5, y: 0.48 },
  { id: 'RCM', x: 0.72, y: 0.5 },
  { id: 'LW', x: 0.16, y: 0.74 },
  { id: 'ST', x: 0.5, y: 0.82 },
  { id: 'RW', x: 0.84, y: 0.74 },
]

const FORMATION_4231: PitchSlot[] = [
  { id: 'GK', x: 0.5, y: 0.08 },
  { id: 'LB', x: 0.12, y: 0.28 },
  { id: 'LCB', x: 0.36, y: 0.26 },
  { id: 'RCB', x: 0.64, y: 0.26 },
  { id: 'RB', x: 0.88, y: 0.28 },
  { id: 'LDM', x: 0.38, y: 0.46 },
  { id: 'RDM', x: 0.62, y: 0.46 },
  { id: 'LAM', x: 0.18, y: 0.66 },
  { id: 'CAM', x: 0.5, y: 0.64 },
  { id: 'RAM', x: 0.82, y: 0.66 },
  { id: 'ST', x: 0.5, y: 0.84 },
]

const LATERAL: ReadonlySet<Position> = new Set([
  'RB',
  'LB',
  'RWB',
  'LWB',
  'RM',
  'LM',
  'RW',
  'LW',
  'CDM',
  'CAM',
  'CF',
])

const SLOT_PREFERENCE: Record<PitchSlotId, Position[]> = {
  GK: ['GK'],
  LB: ['LB', 'LWB', 'LM'],
  LCB: ['CB'],
  RCB: ['CB'],
  RB: ['RB', 'RWB', 'RM'],
  LDM: ['CDM', 'CM'],
  CDM: ['CDM', 'CM'],
  RDM: ['CDM', 'CM'],
  LCM: ['CM', 'CDM', 'LM', 'CAM'],
  CM: ['CM', 'CDM', 'CAM'],
  RCM: ['CM', 'CDM', 'RM', 'CAM'],
  LAM: ['LW', 'LM', 'CAM'],
  CAM: ['CAM', 'CM', 'CF'],
  RAM: ['RW', 'RM', 'CAM'],
  LW: ['LW', 'LM', 'CF'],
  ST: ['ST', 'CF'],
  RW: ['RW', 'RM', 'CF'],
}

/** True when the squad has enough lateral/role detail to place an XI on a pitch. */
export function hasRolePrecision(squad: PlayerSeason[]): boolean {
  const top = [...squad]
    .sort((a, b) => b.minutesPlayed - a.minutesPlayed)
    .slice(0, 18)
  const lateral = top.filter((row) => LATERAL.has(row.primaryPosition)).length
  const distinct = new Set(top.map((row) => row.primaryPosition))
  // Need real wide/full-back/advanced roles — not only GK/CB/CM/ST buckets.
  return lateral >= 3 && distinct.size >= 5
}

function pickFormation(squad: PlayerSeason[]): FormationId {
  const counts = squad.reduce<Record<string, number>>((acc, row) => {
    acc[row.primaryPosition] = (acc[row.primaryPosition] ?? 0) + 1
    return acc
  }, {})
  const attackingMids = (counts.CAM ?? 0) + (counts.CDM ?? 0)
  if (attackingMids >= 3 || (counts.CAM ?? 0) >= 1) {
    return '4-2-3-1'
  }
  return '4-3-3'
}

function scoreForSlot(player: PlayerSeason, slotId: PitchSlotId): number {
  const prefs = SLOT_PREFERENCE[slotId]
  const rank = prefs.indexOf(player.primaryPosition)
  if (rank === -1) {
    return -1
  }
  return 1000 - rank * 40 + Math.min(player.minutesPlayed, 4000) / 40
}

/**
 * Build a starting XI from top minutes, assigned into a formation heuristic.
 * Returns null when role precision is insufficient.
 */
export function buildPitchLineup(squad: PlayerSeason[]): {
  formation: FormationId
  placements: PitchPlacement[]
} | null {
  if (!hasRolePrecision(squad)) {
    return null
  }

  const pool = [...squad].sort((a, b) => b.minutesPlayed - a.minutesPlayed).slice(0, 18)
  const formation = pickFormation(pool)
  const slots = formation === '4-2-3-1' ? FORMATION_4231 : FORMATION_433
  const remaining = [...pool]
  const placements: PitchPlacement[] = []

  for (const slot of slots) {
    let bestIndex = -1
    let bestScore = -1
    for (let i = 0; i < remaining.length; i += 1) {
      const score = scoreForSlot(remaining[i], slot.id)
      if (score > bestScore) {
        bestScore = score
        bestIndex = i
      }
    }
    if (bestIndex === -1) {
      // Fill leftover slots with next highest-minute unused player.
      if (remaining.length === 0) {
        break
      }
      const fallback = remaining.shift()!
      placements.push({ slot, player: fallback })
      continue
    }
    const [player] = remaining.splice(bestIndex, 1)
    placements.push({ slot, player })
  }

  if (placements.length < 11) {
    return null
  }
  return { formation, placements }
}

export function shortDisplayName(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length <= 1) {
    return fullName
  }
  return parts[parts.length - 1]
}
