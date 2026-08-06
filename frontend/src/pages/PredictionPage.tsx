import { Stack } from '@mui/material'
import { useSearchParams } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { PredictionForm } from '@/components/prediction/PredictionForm'

export function PredictionPage() {
  const [params] = useSearchParams()
  const playerId = params.get('playerId') ?? undefined
  const clubId = params.get('clubId') ?? undefined

  return (
    <Stack spacing={0}>
      <PageHeader
        description="Ask how a player is likely to perform after joining a club for a given season. Results include explainable factors, not just a score."
        title="Transfer prediction"
      />
      <PredictionForm initialClubId={clubId} initialPlayerId={playerId} />
    </Stack>
  )
}
