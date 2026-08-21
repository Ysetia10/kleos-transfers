import { Box, Button, Dialog, DialogContent, DialogTitle, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { PredictionForm } from '@/components/prediction/PredictionForm'
import { ModelAccuracySection } from '@/components/home/ModelAccuracySection'

export function HomePage() {
  const [params] = useSearchParams()
  const playerId = params.get('playerId') ?? undefined
  const clubId = params.get('clubId') ?? undefined
  const [simulatorOpen, setSimulatorOpen] = useState(false)

  return (
    <Stack spacing={{ xs: 4, md: 5 }}>
      <Stack spacing={2.5}>
        <PageHeader
          description="Select a professional athlete and target club to generate data-driven performance forecasts for the upcoming season."
          descriptionSx={{ whiteSpace: { xs: 'normal', md: 'nowrap' } }}
          eyebrow="Analysis engine"
          title="Player Projections"
        />

        <SurfaceCard
          sx={{
            p: { xs: 2.5, md: 3 },
            borderRadius: 2,
            boxShadow: 'none',
          }}
        >
          <PredictionForm
            key={`${playerId ?? ''}:${clubId ?? ''}:upcoming`}
            initialClubId={clubId}
            initialPlayerId={playerId}
            seasonMode="upcoming"
          />
        </SurfaceCard>
      </Stack>

      <ModelAccuracySection />

      <Box
        sx={{
          alignItems: { xs: 'stretch', md: 'center' },
          backgroundColor: '#0F172A',
          borderRadius: 2,
          display: 'flex',
          flexDirection: { xs: 'column', md: 'row' },
          gap: 2.5,
          justifyContent: 'space-between',
          px: { xs: 2.5, md: 3.5 },
          py: { xs: 3, md: 3.5 },
        }}
      >
        <Stack spacing={0.75} sx={{ maxWidth: 640 }}>
          <Typography sx={{ color: '#F8FAFC', fontWeight: 500 }} variant="h5">
            System Simulator
          </Typography>
          <Typography sx={{ color: 'rgba(248, 250, 252, 0.72)' }} variant="body2">
            Validate the engine by running historical predictions on past completed seasons and
            comparing them against finalized data.
          </Typography>
        </Stack>
        <Button
          onClick={() => setSimulatorOpen(true)}
          sx={{
            alignSelf: { xs: 'stretch', md: 'center' },
            borderColor: 'rgba(248, 250, 252, 0.35)',
            color: '#F8FAFC',
            flexShrink: 0,
            px: 2.5,
            '&:hover': {
              borderColor: '#F8FAFC',
              backgroundColor: 'rgba(248, 250, 252, 0.06)',
            },
          }}
          variant="outlined"
        >
          Run Historical Validation
        </Button>
      </Box>

      <Dialog
        fullWidth
        maxWidth="md"
        onClose={() => setSimulatorOpen(false)}
        open={simulatorOpen}
      >
        <DialogTitle>Simulator — previous seasons</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary" sx={{ mb: 2 }} variant="body2">
            Choose a completed season, then run a prediction. When outcomes exist, the result page
            shows predicted vs actual stats.
          </Typography>
          <PredictionForm
            key={`${playerId ?? ''}:${clubId ?? ''}:historical`}
            initialClubId={clubId}
            initialPlayerId={playerId}
            seasonMode="historical"
          />
        </DialogContent>
      </Dialog>
    </Stack>
  )
}
