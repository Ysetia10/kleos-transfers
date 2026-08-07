import { Stack } from '@mui/material'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { PredictionForm } from '@/components/prediction/PredictionForm'
import { RecentPredictionsSection } from '@/components/home/RecentPredictionsSection'

export function HomePage() {
  const [params] = useSearchParams()
  const playerId = params.get('playerId') ?? undefined
  const clubId = params.get('clubId') ?? undefined

  return (
    <Stack spacing={4}>
      <PageHeader
        description="Pick a player, destination, and season. Projected minutes and contribution come with factor-level explanations."
        eyebrow="Prediction workspace"
        title="Transfer Simulator"
      />

      <SurfaceCard>
        <PredictionForm
          key={`${playerId ?? ''}:${clubId ?? ''}`}
          initialClubId={clubId}
          initialPlayerId={playerId}
          showSquad
        />
      </SurfaceCard>

      <RecentPredictionsSection />
    </Stack>
  )
}
