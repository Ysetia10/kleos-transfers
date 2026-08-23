import { Box, Stack, Typography } from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { Link as RouterLink } from 'react-router-dom'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getLikelyLineup } from '@/services/club/likelyLineupApi'
import type { LikelyLineupPlacement } from '@/services/club/likelyLineupApi'
import type { PlayerSeason } from '@/types/domain'
import {
  buildPitchLineup,
  shortDisplayName,
  type PitchPlacement,
  type PitchSlotId,
} from '@/utils/pitchLineup'

type PitchLineupProps = {
  clubId?: string
  seasonId?: string
  squad?: PlayerSeason[]
  title?: string
}

function formatMinutes(minutes: number): string {
  if (minutes >= 1000) {
    return `${(minutes / 1000).toFixed(1).replace(/\.0$/, '')}k`
  }
  return String(minutes)
}

function placementFromApi(row: LikelyLineupPlacement): PitchPlacement {
  return {
    slot: {
      id: row.slotId as PitchSlotId,
      x: row.x,
      y: row.y,
    },
    player: row.player,
    likelyStarter: row.likelyStarter,
  }
}

export function PitchLineup({
  clubId,
  seasonId,
  squad,
  title = 'Likely XI',
}: PitchLineupProps) {
  const apiQuery = useQuery({
    queryKey: queryKeys.clubs.likelyLineup(clubId ?? '', seasonId ?? ''),
    queryFn: () => getLikelyLineup(clubId!, seasonId!),
    enabled: !!clubId && !!seasonId,
  })

  const clientLineup = squad?.length ? buildPitchLineup(squad) : null
  const apiReady =
    apiQuery.data?.rolePrecisionAvailable && apiQuery.data.placements.length >= 11

  const formation = apiReady ? apiQuery.data!.formation : clientLineup?.formation
  const placements: PitchPlacement[] = apiReady
    ? apiQuery.data!.placements.map(placementFromApi)
    : clientLineup?.placements ?? []

  if (!formation || placements.length < 11) {
    return null
  }

  return (
    <Stack spacing={1.5}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        sx={{ alignItems: { sm: 'baseline' }, justifyContent: 'space-between' }}
      >
        <Typography variant="h4">{title}</Typography>
        <Typography color="text.secondary" variant="caption">
          Formation {formation} · inferred from minutes · photo + name
        </Typography>
      </Stack>
      <Box
        sx={{
          position: 'relative',
          width: '100%',
          maxWidth: 560,
          mx: 'auto',
          aspectRatio: '3 / 4',
          borderRadius: 3,
          overflow: 'hidden',
          background: (theme) =>
            theme.palette.mode === 'dark'
              ? 'linear-gradient(180deg, #14532d 0%, #166534 45%, #15803d 100%)'
              : 'linear-gradient(180deg, #16a34a 0%, #22c55e 45%, #4ade80 100%)',
          border: '1px solid',
          borderColor: 'divider',
          boxShadow: (theme) => `inset 0 0 0 1px ${theme.palette.pitch.mist}`,
        }}
      >
        {/* Pitch markings */}
        <Box
          sx={{
            position: 'absolute',
            inset: '6%',
            border: '2px solid rgba(255,255,255,0.35)',
            borderRadius: 1,
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            left: '50%',
            top: '50%',
            width: '28%',
            aspectRatio: '1',
            border: '2px solid rgba(255,255,255,0.35)',
            borderRadius: '50%',
            transform: 'translate(-50%, -50%)',
            pointerEvents: 'none',
          }}
        />
        <Box
          sx={{
            position: 'absolute',
            left: '50%',
            top: '50%',
            width: 6,
            height: 6,
            borderRadius: '50%',
            backgroundColor: 'rgba(255,255,255,0.55)',
            transform: 'translate(-50%, -50%)',
            pointerEvents: 'none',
          }}
        />

        {placements.map(({ slot, player, likelyStarter = true }) => (
          <Box
            key={`${slot.id}-${player.playerId}`}
            component={RouterLink}
            to={routes.playerDetail(player.playerId)}
            sx={{
              position: 'absolute',
              left: `${slot.x * 100}%`,
              top: `${(1 - slot.y) * 100}%`,
              transform: 'translate(-50%, -50%)',
              textDecoration: 'none',
              color: 'inherit',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 0.25,
              width: 72,
              zIndex: 1,
              opacity: likelyStarter ? 1 : 0.72,
            }}
          >
            <IdentityMedia
              imageUrl={player.photoUrl}
              label={player.playerName}
              size={44}
            />
            <Typography
              sx={{
                color: '#fff',
                textShadow: '0 1px 2px rgba(0,0,0,0.55)',
                fontWeight: 500,
                lineHeight: 1.15,
                textAlign: 'center',
                fontSize: 11,
              }}
            >
              {shortDisplayName(player.playerName)}
            </Typography>
            <Typography
              sx={{
                color: 'rgba(255,255,255,0.88)',
                textShadow: '0 1px 2px rgba(0,0,0,0.55)',
                lineHeight: 1.1,
                textAlign: 'center',
                fontSize: 10,
              }}
            >
              {formatMinutes(player.minutesPlayed)} min
            </Typography>
          </Box>
        ))}
      </Box>
    </Stack>
  )
}
