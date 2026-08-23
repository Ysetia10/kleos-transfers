import type { PlayerSeason, Position } from '@/types/domain'

export type FormationId = '4-3-3' | '4-2-3-1' | '3-4-3' | '3-5-2'

export type PitchSlotId =
  | 'GK'
  | 'LB'
  | 'LCB'
  | 'CB'
  | 'RCB'
  | 'RB'
  | 'LWB'
  | 'RWB'
  | 'LDM'
  | 'CDM'
  | 'RDM'
  | 'LCM'
  | 'CM'
  | 'RCM'
  | 'LM'
  | 'RM'
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
  likelyStarter?: boolean
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

/** Three centre-backs, wide mids (RM/LM) — fits asymmetric full-back usage (e.g. one RB, no LB). */
const FORMATION_343: PitchSlot[] = [
  { id: 'GK', x: 0.5, y: 0.08 },
  { id: 'LCB', x: 0.22, y: 0.26 },
  { id: 'CB', x: 0.5, y: 0.24 },
  { id: 'RCB', x: 0.78, y: 0.26 },
  { id: 'LM', x: 0.14, y: 0.5 },
  { id: 'LCM', x: 0.38, y: 0.48 },
  { id: 'RCM', x: 0.62, y: 0.48 },
  { id: 'RM', x: 0.86, y: 0.5 },
  { id: 'LW', x: 0.2, y: 0.74 },
  { id: 'ST', x: 0.5, y: 0.82 },
  { id: 'RW', x: 0.8, y: 0.74 },
]

const FORMATION_352: PitchSlot[] = [
  { id: 'GK', x: 0.5, y: 0.08 },
  { id: 'LCB', x: 0.22, y: 0.26 },
  { id: 'CB', x: 0.5, y: 0.24 },
  { id: 'RCB', x: 0.78, y: 0.26 },
  { id: 'LWB', x: 0.1, y: 0.44 },
  { id: 'RWB', x: 0.9, y: 0.44 },
  { id: 'LCM', x: 0.32, y: 0.52 },
  { id: 'CM', x: 0.5, y: 0.5 },
  { id: 'RCM', x: 0.68, y: 0.52 },
  { id: 'ST', x: 0.38, y: 0.8 },
  { id: 'ST', x: 0.62, y: 0.8 },
]

const FORMATIONS: Record<FormationId, PitchSlot[]> = {
  '4-3-3': FORMATION_433,
  '4-2-3-1': FORMATION_4231,
  '3-4-3': FORMATION_343,
  '3-5-2': FORMATION_352,
}

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
  CB: ['CB'],
  RCB: ['CB'],
  RB: ['RB', 'RWB', 'RM'],
  LWB: ['LWB', 'LB', 'LM'],
  RWB: ['RWB', 'RB', 'RM'],
  LDM: ['CDM', 'CM'],
  CDM: ['CDM', 'CM'],
  RDM: ['CDM', 'CM'],
  LCM: ['CM', 'CDM', 'LM', 'CAM'],
  CM: ['CM', 'CDM', 'CAM'],
  RCM: ['CM', 'CDM', 'RM', 'CAM'],
  LM: ['LM', 'LW', 'LWB'],
  RM: ['RM', 'RW', 'RWB', 'RB'],
  LAM: ['LW', 'LM', 'CAM'],
  CAM: ['CAM', 'CM', 'CF'],
  RAM: ['RW', 'RM', 'CAM'],
  LW: ['LW', 'LM', 'CF'],
  ST: ['ST', 'CF'],
  RW: ['RW', 'RM', 'CF'],
}

/** Minimum minutes to count as a plausible starter in any XI. */
const STARTER_FLOOR = 900

/** Share of the best minute-total in a side/role band required to count as a regular starter. */
const STARTER_SHARE = 0.45

const FORMATION_TIE_BREAK: FormationId[] = ['4-3-3', '4-2-3-1', '3-4-3', '3-5-2']
const FORMATION_MINUTE_TOLERANCE = 0.92

function isArrival(player: PlayerSeason): boolean {
  return player.inboundTransfer != null
}

function incumbentSquad(squad: PlayerSeason[]): PlayerSeason[] {
  return squad.filter((row) => !isArrival(row))
}

function hasBothFlankStarters(squad: PlayerSeason[]): boolean {
  return hasFlankStarter(squad, 'LEFT') && hasFlankStarter(squad, 'RIGHT')
}

function hasFlankStarter(squad: PlayerSeason[], flank: SideBand): boolean {
  return squad.some(
    (row) => sideBand(row.primaryPosition) === flank && isLikelyStarter(row, squad)
  )
}

function isBackFourFormation(id: FormationId): boolean {
  return id === '4-3-3' || id === '4-2-3-1'
}

type SideBand = 'GK' | 'CB' | 'LEFT' | 'RIGHT' | 'MID' | 'ATTACK'

const SIDE_BAND: Record<Position, SideBand> = {
  GK: 'GK',
  CB: 'CB',
  LB: 'LEFT',
  LWB: 'LEFT',
  LM: 'LEFT',
  LW: 'LEFT',
  RB: 'RIGHT',
  RWB: 'RIGHT',
  RM: 'RIGHT',
  RW: 'RIGHT',
  CDM: 'MID',
  CM: 'MID',
  CAM: 'MID',
  CF: 'ATTACK',
  ST: 'ATTACK',
}

function sideBand(position: Position): SideBand {
  return SIDE_BAND[position]
}

/** Minutes threshold for a player to be treated as a regular starter on their flank/role band. */
export function starterMinutesThreshold(player: PlayerSeason, squad: PlayerSeason[]): number {
  const band = sideBand(player.primaryPosition)
  const peers = squad.filter((row) => sideBand(row.primaryPosition) === band)
  if (peers.length === 0) {
    return STARTER_FLOOR
  }
  const peak = Math.max(...peers.map((row) => row.minutesPlayed))
  return Math.max(STARTER_FLOOR, Math.round(peak * STARTER_SHARE))
}

/** True when season minutes suggest this player was in the XI most weeks (not a cup/backup). */
export function isLikelyStarter(player: PlayerSeason, squad: PlayerSeason[]): boolean {
  return player.minutesPlayed >= starterMinutesThreshold(player, squad)
}

/** True when the squad has enough lateral/role detail to place an XI on a pitch. */
export function hasRolePrecision(squad: PlayerSeason[]): boolean {
  const top = [...squad]
    .sort((a, b) => b.minutesPlayed - a.minutesPlayed)
    .slice(0, 18)
  const lateral = top.filter((row) => LATERAL.has(row.primaryPosition)).length
  const distinct = new Set(top.map((row) => row.primaryPosition))
  return lateral >= 3 && distinct.size >= 5
}

function isWideDefenderSlot(slotId: PitchSlotId): boolean {
  return slotId === 'LB' || slotId === 'RB' || slotId === 'LWB' || slotId === 'RWB'
}

function hasFlankPositionStarter(squad: PlayerSeason[], slotId: PitchSlotId): boolean {
  if (slotId === 'LB' || slotId === 'LWB') {
    return squad.some(
      (row) =>
        (row.primaryPosition === 'LB' ||
          row.primaryPosition === 'LWB' ||
          row.primaryPosition === 'LM') &&
        isLikelyStarter(row, squad)
    )
  }
  if (slotId === 'RB' || slotId === 'RWB') {
    return squad.some(
      (row) =>
        (row.primaryPosition === 'RB' ||
          row.primaryPosition === 'RWB' ||
          row.primaryPosition === 'RM') &&
        isLikelyStarter(row, squad)
    )
  }
  return false
}

function scoreForSlot(player: PlayerSeason, slotId: PitchSlotId, squad: PlayerSeason[]): number {
  const prefs = SLOT_PREFERENCE[slotId]
  let rank = prefs.indexOf(player.primaryPosition)
  if (rank === -1) {
    if (player.primaryPosition === 'CB' && isWideDefenderSlot(slotId)) {
      if (hasFlankPositionStarter(squad, slotId)) {
        return -1
      }
      rank = prefs.length
    } else {
      return -1
    }
  }
  const roleFit = 1000 - rank * 40
  const minuteWeight = Math.min(player.minutesPlayed, 3600) / 3600
  const threshold = starterMinutesThreshold(player, squad)
  if (player.minutesPlayed < threshold) {
    // Backup-tier minutes — only fill a slot if nothing better exists.
    return roleFit * minuteWeight * 0.12
  }
  return roleFit + minuteWeight * 600
}

function pickFormation(squad: PlayerSeason[]): FormationId {
  const requireBackFour = hasBothFlankStarters(squad)
  const candidates: { id: FormationId; totalMinutes: number; starterCount: number }[] = []
  let bestMinutes = -1

  for (const [id, slots] of Object.entries(FORMATIONS) as [FormationId, PitchSlot[]][]) {
    if (requireBackFour && !isBackFourFormation(id)) {
      continue
    }
    const { placements, totalMinutes } = assignFormation(slots, squad)
    if (placements.length < 11) {
      continue
    }
    bestMinutes = Math.max(bestMinutes, totalMinutes)
    const starterCount = placements.filter((row) => isLikelyStarter(row.player, squad)).length
    candidates.push({ id, totalMinutes, starterCount })
  }

  if (candidates.length === 0) {
    return '4-3-3'
  }

  const minuteThreshold = Math.round(bestMinutes * FORMATION_MINUTE_TOLERANCE)
  return candidates
    .filter((row) => row.totalMinutes >= minuteThreshold)
    .sort((a, b) => {
      if (b.totalMinutes !== a.totalMinutes) {
        return b.totalMinutes - a.totalMinutes
      }
      if (b.starterCount !== a.starterCount) {
        return b.starterCount - a.starterCount
      }
      return FORMATION_TIE_BREAK.indexOf(a.id) - FORMATION_TIE_BREAK.indexOf(b.id)
    })[0].id
}

function resolvePlayerForSlot(
  priorPlayer: PlayerSeason,
  slotId: PitchSlotId,
  projected: PlayerSeason[],
  arrivals: PlayerSeason[],
  incumbents: PlayerSeason[],
  priorSquad: PlayerSeason[],
  used: Set<string>
): PlayerSeason | null {
  const incumbent = projected.find((row) => row.playerId === priorPlayer.playerId)
  if (incumbent && !isArrival(incumbent) && !used.has(incumbent.playerId)) {
    return incumbent
  }
  return (
    findReplacement(slotId, arrivals, priorSquad, used)
    ?? findReplacement(slotId, incumbents, priorSquad, used)
  )
}

function findReplacement(
  slotId: PitchSlotId,
  candidates: PlayerSeason[],
  priorSquad: PlayerSeason[],
  used: Set<string>
): PlayerSeason | null {
  let best: PlayerSeason | null = null
  let bestScore = -1
  for (const candidate of candidates) {
    if (used.has(candidate.playerId)) {
      continue
    }
    const score = scoreForSlot(candidate, slotId, priorSquad)
    if (score > bestScore) {
      bestScore = score
      best = candidate
    }
  }
  return best
}

function buildContinuityLineup(
  priorSquad: PlayerSeason[],
  projectedSquad: PlayerSeason[]
): { formation: FormationId; placements: PitchPlacement[] } | null {
  if (!hasRolePrecision(priorSquad)) {
    return null
  }

  const formation = pickFormation(priorSquad)
  const slots = FORMATIONS[formation]
  const { placements: baseline } = assignFormation(slots, topPool(priorSquad), priorSquad)
  if (baseline.length < 11) {
    return null
  }

  const arrivals = projectedSquad.filter(isArrival)
  const incumbents = incumbentSquad(projectedSquad)
  const used = new Set<string>()
  const placements: PitchPlacement[] = []

  for (const baselineSlot of baseline) {
    const chosen = resolvePlayerForSlot(
      baselineSlot.player,
      baselineSlot.slot.id,
      projectedSquad,
      arrivals,
      incumbents,
      priorSquad,
      used
    )
    if (!chosen) {
      return null
    }
    used.add(chosen.playerId)
    placements.push({
      slot: baselineSlot.slot,
      player: chosen,
      likelyStarter: isLikelyStarter(chosen, incumbents.length ? incumbents : priorSquad),
    })
  }

  return { formation, placements }
}

function topPool(squad: PlayerSeason[]): PlayerSeason[] {
  return [...squad].sort((a, b) => b.minutesPlayed - a.minutesPlayed).slice(0, 20)
}

function assignFormation(
  slots: PitchSlot[],
  squad: PlayerSeason[],
  minuteReference: PlayerSeason[] = squad
): { placements: PitchPlacement[]; totalMinutes: number } {
  const remaining = [...squad].sort((a, b) => b.minutesPlayed - a.minutesPlayed)
  const placements: PitchPlacement[] = []

  for (const slot of slots) {
    let bestIndex = -1
    let bestScore = -1
    for (let i = 0; i < remaining.length; i += 1) {
      const score = scoreForSlot(remaining[i], slot.id, minuteReference)
      if (score > bestScore) {
        bestScore = score
        bestIndex = i
      }
    }
    if (bestIndex === -1) {
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

  const totalMinutes = placements.reduce((sum, row) => sum + row.player.minutesPlayed, 0)
  return { placements, totalMinutes }
}

/**
 * Build a likely starting XI from last season's minutes at the club, then apply transfer
 * continuity on the projected squad. New signings never drive formation selection.
 */
export function buildPitchLineup(squad: PlayerSeason[]): {
  formation: FormationId
  placements: PitchPlacement[]
} | null {
  const incumbents = incumbentSquad(squad)
  const baseline = incumbents.length ? incumbents : squad
  return buildContinuityLineup(baseline, squad)
}

export function shortDisplayName(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean)
  if (parts.length <= 1) {
    return fullName
  }
  return parts[parts.length - 1]
}
