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
        description="Example: Anthony Gordon → Barcelona in 2026/27 uses Barça’s latest completed roster (2025/26) for squad competition — not an empty upcoming XI. Same as-of engine we backtest on finished seasons."
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
