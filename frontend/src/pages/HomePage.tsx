import { Button, Stack, Typography } from '@mui/material'
import { useEffect } from 'react'
import { Link as RouterLink, useLocation, useSearchParams } from 'react-router-dom'
import { CatalogueSection } from '@/components/home/CatalogueSection'
import { HomeSection } from '@/components/home/HomeSection'
import {
  AllTimeLeadersSection,
  TrendingPlayersSection,
} from '@/components/home/LeaderboardSection'
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
      <Stack spacing={2.5} sx={{ maxWidth: 760 }}>
        <Typography component="h1" variant="h1">
          Kleos Transfers
        </Typography>
        <Typography color="text.secondary" variant="body1">
          One workspace for transfer what-ifs (including historical seasons), league leaders, and
          the player/club catalogue behind every scenario.
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
        description="Run a player → club scenario for any season in the catalogue. Historical seasons act as a simulator (context as of season start). When you pick a club and season, the squad table below the form shows that season's roster — not just a same-position count."
        id={homeSections.predict}
        title="Transfer simulator"
      >
        <PredictionForm
          key={`${playerId ?? ''}:${clubId ?? ''}`}
          initialClubId={clubId}
          initialPlayerId={playerId}
          showSquad
        />
      </HomeSection>

      <TrendingPlayersSection />
      <AllTimeLeadersSection />
      <RecentPredictionsSection />
      <CatalogueSection />
    </Stack>
  )
}
