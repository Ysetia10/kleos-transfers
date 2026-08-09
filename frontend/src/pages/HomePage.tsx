import { Button, Dialog, DialogContent, DialogTitle, Stack, Typography } from '@mui/material'
import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { PredictionForm } from '@/components/prediction/PredictionForm'
import { RecentPredictionsSection } from '@/components/home/RecentPredictionsSection'

export function HomePage() {
  const [params] = useSearchParams()
  const playerId = params.get('playerId') ?? undefined
  const clubId = params.get('clubId') ?? undefined
  const [historicalOpen, setHistoricalOpen] = useState(false)

  return (
    <Stack spacing={4}>
      <PageHeader
        description="Project a player into the current campaign using the destination club’s latest completed roster ± window transfers."
        eyebrow="Prediction workspace"
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

      <Stack spacing={1} sx={{ alignItems: { sm: 'flex-start' } }}>
        <Button onClick={() => setHistoricalOpen(true)} variant="outlined">
          Predict for previous seasons
        </Button>
        <Typography color="text.secondary" variant="body2">
          Open the backtest path for completed campaigns (actuals available for evaluation).
        </Typography>
      </Stack>

      <Dialog
        fullWidth
        maxWidth="md"
        onClose={() => setHistoricalOpen(false)}
        open={historicalOpen}
      >
        <DialogTitle>Predict for previous seasons</DialogTitle>
        <DialogContent>
          <Typography color="text.secondary" sx={{ mb: 2 }} variant="body2">
            Choose a completed season to project into. Use this when you want to validate the engine
            against known outcomes.
          </Typography>
          <PredictionForm
            key={`${playerId ?? ''}:${clubId ?? ''}:historical`}
            initialClubId={clubId}
            initialPlayerId={playerId}
            seasonMode="historical"
          />
        </DialogContent>
      </Dialog>

      <RecentPredictionsSection />
    </Stack>
  )
}
