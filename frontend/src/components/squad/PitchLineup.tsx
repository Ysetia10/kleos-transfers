import { Box, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { routes } from '@/constants/routes'
import type { PlayerSeason } from '@/types/domain'
import {
  buildPitchLineup,
  shortDisplayName,
} from '@/utils/pitchLineup'

type PitchLineupProps = {
  squad: PlayerSeason[] | undefined
  title?: string
}

export function PitchLineup({ squad, title = 'Starting XI by minutes' }: PitchLineupProps) {
  if (!squad?.length) {
    return null
  }

  const lineup = buildPitchLineup(squad)
  if (!lineup) {
    return (
      <SurfaceCard accent="info">
        <Typography variant="h4">{title}</Typography>
        <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
          Role precision unavailable for this squad — FBref season tables only stored coarse
          GK/CB/CM/ST buckets. Run{' '}
          <Box component="span" sx={{ fontFamily: 'ui-monospace, monospace' }}>
            scripts/enrich_positions_from_fbref_lineups.py
          </Box>{' '}
          to place players left/right on the pitch. Minutes table remains below.
        </Typography>
      </SurfaceCard>
    )
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
          Formation {lineup.formation} · photo + name
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

        {lineup.placements.map(({ slot, player }) => (
          <Box
            key={slot.id}
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
              gap: 0.5,
              width: 72,
              zIndex: 1,
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
                fontWeight: 600,
                lineHeight: 1.15,
                textAlign: 'center',
                fontSize: 11,
              }}
            >
              {shortDisplayName(player.playerName)}
            </Typography>
          </Box>
        ))}
      </Box>
    </Stack>
  )
}
