import { Button, Dialog, DialogContent, DialogTitle, Stack, Typography } from '@mui/material'
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
    <Stack spacing={4}>
      <PageHeader
        description="Pick a player and a destination club to project next season’s minutes, goals, and assists — using the club’s latest completed squad plus confirmed transfer updates."
        eyebrow="Prediction"
        title="Prediction"
      />

      <SurfaceCard>
        <PredictionForm
          key={`${playerId ?? ''}:${clubId ?? ''}:upcoming`}
          initialClubId={clubId}
          initialPlayerId={playerId}
          seasonMode="upcoming"
        />
      </SurfaceCard>

      <ModelAccuracySection />

      <Stack spacing={1.5}>
        <Typography variant="h5">Simulator</Typography>
        <Typography color="text.secondary" variant="body2">
          Run the same engine on a completed season to compare predicted minutes and output with what
          actually happened.
        </Typography>
        <Button
          onClick={() => setSimulatorOpen(true)}
          sx={{ alignSelf: { sm: 'flex-start' } }}
          variant="outlined"
        >
          Predict for previous seasons
        </Button>
      </Stack>

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
