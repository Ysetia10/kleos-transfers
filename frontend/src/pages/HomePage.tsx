import { Box, Button, Stack, Typography } from '@mui/material'
import { useEffect } from 'react'
import { Link as RouterLink, useLocation, useSearchParams } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
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
    <Stack spacing={{ xs: 4, md: 5 }}>
      <Box
        sx={(theme) => ({
          position: 'relative',
          overflow: 'hidden',
          borderRadius: 16,
          color: theme.palette.primary.contrastText,
          px: { xs: 3, md: 5 },
          py: { xs: 4.5, md: 6 },
          background: `
            linear-gradient(135deg, ${theme.palette.pitch.deep} 0%, ${theme.palette.primary.main} 55%, #244a38 100%)
          `,
          boxShadow: `0 24px 48px rgba(15, 36, 28, 0.22)`,
          animation: 'kleos-rise 500ms ease both',
          '&::after': {
            content: '""',
            position: 'absolute',
            inset: 0,
            opacity: 0.35,
            backgroundImage: `
              repeating-linear-gradient(
                90deg,
                transparent 0,
                transparent 36px,
                rgba(255,255,255,0.05) 36px,
                rgba(255,255,255,0.05) 37px
              ),
              repeating-linear-gradient(
                0deg,
                transparent 0,
                transparent 36px,
                rgba(255,255,255,0.04) 36px,
                rgba(255,255,255,0.04) 37px
              )
            `,
          },
        })}
      >
        <Stack spacing={2.5} sx={{ position: 'relative', zIndex: 1, maxWidth: 720 }}>
          <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
            <Box sx={{ color: 'accent.light' }}>
              <BrandMark animated size={36} />
            </Box>
            <Typography
              sx={{
                color: 'accent.light',
                fontFamily: '"Barlow Condensed", sans-serif',
                fontWeight: 600,
                letterSpacing: '0.14em',
                textTransform: 'uppercase',
              }}
              variant="caption"
            >
              Football transfer intelligence
            </Typography>
          </Stack>
          <Typography component="h1" sx={{ color: 'inherit' }} variant="display">
            Kleos Transfers
          </Typography>
          <Typography sx={{ color: 'rgba(247, 250, 247, 0.82)', maxWidth: 560 }} variant="bodyLarge">
            Model a move, read the squad around it, and browse league leaders — one pitch-side
            workspace, not a maze of pages.
          </Typography>
          <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', pt: 0.5 }} useFlexGap>
            {homeJumpLinks.map(({ label, section }) => {
              const primary = section === homeSections.predict
              return (
                <Button
                  component={RouterLink}
                  key={section}
                  size="medium"
                  to={{ pathname: routes.home, hash: `#${section}`, search: location.search }}
                  variant={primary ? 'contained' : 'outlined'}
                  color={primary ? 'accent' : 'inherit'}
                  sx={
                    primary
                      ? undefined
                      : {
                          borderColor: 'rgba(247,250,247,0.35)',
                          color: 'rgba(247,250,247,0.92)',
                          '&:hover': {
                            borderColor: 'accent.light',
                            backgroundColor: 'rgba(196, 154, 60, 0.12)',
                          },
                        }
                  }
                >
                  {label}
                </Button>
              )
            })}
          </Stack>
        </Stack>
      </Box>

      <HomeSection
        description="Pick a player, destination, and season. Historical seasons work as a simulator; the squad table shows the full roster for that campaign."
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
