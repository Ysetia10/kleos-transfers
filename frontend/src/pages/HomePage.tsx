import { Button, Stack, Typography } from '@mui/material'
import { useEffect } from 'react'
import { Link as RouterLink, useLocation, useSearchParams } from 'react-router-dom'
import { CatalogueSection } from '@/components/home/CatalogueSection'
import { HomeSection } from '@/components/home/HomeSection'
import { RecentPredictionsSection } from '@/components/home/RecentPredictionsSection'
import { PredictionForm } from '@/components/prediction/PredictionForm'
import { homeJumpLinks, homeSections, routes } from '@/constants/routes'

export function HomePage() {
  const location = useLocation()
  const [params] = useSearchParams()
  const playerId = params.get('playerId') ?? undefined
  const clubId = params.get('clubId') ?? undefined

  useEffect(() => {
    if (!location.hash) {
      return
    }
    const id = decodeURIComponent(location.hash.replace(/^#/, ''))
    const frame = window.requestAnimationFrame(() => {
      document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    })
    return () => window.cancelAnimationFrame(frame)
  }, [location.hash, location.search])

  return (
    <Stack spacing={{ xs: 6, md: 8 }}>
      <Stack spacing={2.5} sx={{ maxWidth: 720 }}>
        <Typography component="h1" variant="h1">
          Kleos Transfers
        </Typography>
        <Typography color="text.secondary" variant="body1">
          Context-aware transfer what-ifs in one place — project minutes and fit after a move,
          review recent runs, and browse the player/club identities behind them.
        </Typography>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }} useFlexGap>
          {homeJumpLinks.map(({ label, section }) => (
            <Button
              component={RouterLink}
              key={section}
              size="small"
              to={{ pathname: routes.home, hash: `#${section}`, search: location.search }}
              variant={section === homeSections.predict ? 'contained' : 'outlined'}
            >
              {label}
            </Button>
          ))}
        </Stack>
      </Stack>

      <HomeSection
        description="Choose a player, destination club, and season. Results include explainable factors, not just a score."
        id={homeSections.predict}
        title="Predict a transfer"
      >
        <PredictionForm
          key={`${playerId ?? ''}:${clubId ?? ''}`}
          initialClubId={clubId}
          initialPlayerId={playerId}
        />
      </HomeSection>

      <RecentPredictionsSection />
      <CatalogueSection />
    </Stack>
  )
}
