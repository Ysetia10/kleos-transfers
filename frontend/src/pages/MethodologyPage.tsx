import { Box, Button, Stack, Typography } from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'
import { PageHeader } from '@/components/common/PageHeader'
import { SurfaceCard } from '@/components/common/SurfaceCard'
import { homePredictPath } from '@/constants/routes'

const pillars = [
  {
    title: 'Project output',
    detail: 'Estimate first-season minutes, goals, assists, and expected contribution for a move.',
  },
  {
    title: 'Expose the why',
    detail: 'Trace the projection to contextual signals — age, role, squad depth, league transition.',
  },
  {
    title: 'Communicate risk',
    detail: 'Surface confidence from data completeness so shortlists stay honest about uncertainty.',
  },
] as const

const steps = [
  {
    code: '01 / Baseline',
    title: 'Player history',
    detail: 'Recent PlayerSeason rates adjusted for position and playing time.',
  },
  {
    code: '02 / Context',
    title: 'Destination fit',
    detail: 'Club, season, and competition signals reshape minutes and contribution.',
  },
  {
    code: '03 / Range',
    title: 'Explainable scores',
    detail: 'Compatibility and confidence summarize fit and input completeness.',
  },
] as const

export function MethodologyPage() {
  return (
    <Stack spacing={4} sx={{ maxWidth: 960 }}>
      <PageHeader
        actions={
          <Button component={RouterLink} to={homePredictPath()} variant="contained">
            Open simulator
          </Button>
        }
        description="Kleos is a heuristic decision layer for transfer hypotheses — readable factors, not a black box."
        eyebrow="Model documentation"
        title="Make every transfer hypothesis defensible."
      />

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: 'repeat(3, minmax(0, 1fr))' },
        }}
      >
        {pillars.map((pillar) => (
          <SurfaceCard key={pillar.title}>
            <Typography variant="h4">{pillar.title}</Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }} variant="body2">
              {pillar.detail}
            </Typography>
          </SurfaceCard>
        ))}
      </Box>

      <SurfaceCard>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{ justifyContent: 'space-between', mb: 2 }}
        >
          <Typography color="accent.main" variant="caption">
            v0.2 heuristic engine
          </Typography>
          <Typography color="primary.main" variant="caption">
            Weighted contextual signals
          </Typography>
        </Stack>
        <Typography sx={{ mb: 3 }} variant="h3">
          How a prediction is assembled
        </Typography>
        <Stack divider={<Box sx={{ borderTop: 1, borderColor: 'divider' }} />} spacing={2}>
          {steps.map((step) => (
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              key={step.code}
              spacing={2}
              sx={{ pt: 2 }}
            >
              <Typography color="primary.main" sx={{ minWidth: 140 }} variant="caption">
                {step.code}
              </Typography>
              <Stack spacing={0.5}>
                <Typography variant="subtitle2">{step.title}</Typography>
                <Typography color="text.secondary" variant="body2">
                  {step.detail}
                </Typography>
              </Stack>
            </Stack>
          ))}
        </Stack>
      </SurfaceCard>

      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
        }}
      >
        <SurfaceCard>
          <Typography variant="h4">Appropriate use</Typography>
          <Typography color="text.secondary" component="ul" sx={{ mt: 1, pl: 2 }} variant="body2">
            <li>Shortlisting destinations for a player</li>
            <li>Comparing completed-season what-ifs</li>
            <li>Explaining minutes risk to non-model stakeholders</li>
          </Typography>
        </SurfaceCard>
        <SurfaceCard>
          <Typography variant="h4">Important limitations</Typography>
          <Typography color="text.secondary" component="ul" sx={{ mt: 1, pl: 2 }} variant="body2">
            <li>xG/xA recovery still incomplete in history</li>
            <li>
              Club fit index is absolute ({`absolute-v0.1`}); player→club fit stays on prediction
              compatibility
            </li>
            <li>Not a substitute for medical or contract diligence</li>
          </Typography>
        </SurfaceCard>
      </Box>
    </Stack>
  )
}
