import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  Chip,
  LinearProgress,
  Stack,
  Typography,
} from '@mui/material'
import type { Explanation, ExplanationDirection } from '@/types/domain'
import {
  absImpact,
  buildPredictionSummary,
  directionTone,
  driverBarColor,
  getTopDrivers,
  getWatchouts,
  groupExplanationsByCategory,
  strengthLabel,
  type CategoryStrength,
  type ExplanationCategoryBundle,
} from '@/utils/explanationPresentation'

interface ExplanationListProps {
  explanations: Explanation[]
}

function directionChipColor(
  direction: ExplanationDirection,
): 'success' | 'error' | 'default' {
  if (direction === 'POSITIVE') {
    return 'success'
  }
  if (direction === 'NEGATIVE') {
    return 'error'
  }
  return 'default'
}

function strengthColor(strength: CategoryStrength): 'success.main' | 'error.main' | 'text.secondary' {
  if (strength === 'supportive') {
    return 'success.main'
  }
  if (strength === 'headwind') {
    return 'error.main'
  }
  return 'text.secondary'
}

function formatAggregate(value: number): string {
  const rounded = Math.round(value * 10) / 10
  if (rounded > 0) {
    return `+${rounded.toFixed(1)}`
  }
  return rounded.toFixed(1)
}

function SignalDetail({ item }: { item: Explanation }) {
  return (
    <Stack
      spacing={0.75}
      sx={{
        borderLeft: (theme) =>
          `2px solid ${
            item.direction === 'POSITIVE'
              ? theme.palette.success.main
              : item.direction === 'NEGATIVE'
                ? theme.palette.error.main
                : theme.palette.divider
          }`,
        pl: 1.25,
        py: 0.25,
      }}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
      >
        <Typography sx={{ fontWeight: 500 }} variant="body2">
          {item.label}
        </Typography>
        <Stack direction="row" spacing={0.75} sx={{ alignItems: 'center', flexShrink: 0 }}>
          <Typography color="text.secondary" variant="caption">
            Impact {Number(item.impact).toFixed(1)}
          </Typography>
          <Chip
            color={directionChipColor(item.direction)}
            label={item.direction.toLowerCase()}
            size="small"
            variant={item.direction === 'POSITIVE' ? 'filled' : 'outlined'}
          />
        </Stack>
      </Stack>
      <Typography color="text.secondary" variant="body2">
        {item.detail}
      </Typography>
    </Stack>
  )
}

function KeyDriversChart({ drivers }: { drivers: Explanation[] }) {
  if (drivers.length === 0) {
    return null
  }

  const maxImpact = Math.max(...drivers.map(absImpact), 1)

  return (
    <Stack spacing={1.25}>
      <Typography color="text.secondary" variant="overline" sx={{ letterSpacing: '0.08em' }}>
        Key drivers
      </Typography>
      <Stack spacing={1}>
        {drivers.map((item) => {
          const widthPct = Math.max(8, (absImpact(item) / maxImpact) * 100)
          return (
            <Stack key={item.id} spacing={0.5}>
              <Stack
                direction="row"
                spacing={1}
                sx={{ alignItems: 'baseline', justifyContent: 'space-between', gap: 1 }}
              >
                <Typography
                  sx={{ fontWeight: 500, minWidth: 0, flex: 1 }}
                  title={item.label}
                  variant="body2"
                  noWrap
                >
                  {item.label}
                </Typography>
                <Typography color="text.secondary" variant="caption" sx={{ flexShrink: 0 }}>
                  {Number(item.impact).toFixed(1)}
                </Typography>
              </Stack>
              <Box
                sx={{
                  bgcolor: 'action.hover',
                  borderRadius: 1,
                  height: 8,
                  overflow: 'hidden',
                  width: '100%',
                }}
              >
                <Box
                  sx={{
                    bgcolor: driverBarColor(item.direction),
                    borderRadius: 1,
                    height: '100%',
                    opacity: item.direction === 'NEUTRAL' ? 0.65 : 1,
                    width: `${widthPct}%`,
                  }}
                />
              </Box>
            </Stack>
          )
        })}
      </Stack>
    </Stack>
  )
}

function CategoryCard({ bundle }: { bundle: ExplanationCategoryBundle }) {
  const hiddenCount = bundle.signals.length - bundle.previewSignals.length

  return (
    <Accordion
      disableGutters
      elevation={0}
      sx={{
        '&::before': { display: 'none' },
        bgcolor: 'background.paper',
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: '12px !important',
        overflow: 'hidden',
      }}
    >
      <AccordionSummary sx={{ px: { xs: 1.5, sm: 2 }, py: 0.5 }}>
        <Stack spacing={0.75} sx={{ minWidth: 0, pr: 1, width: '100%' }}>
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', justifyContent: 'space-between', gap: 1 }}
          >
            <Typography sx={{ fontWeight: 600 }} variant="subtitle2">
              {bundle.title}
            </Typography>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexShrink: 0 }}>
              <Typography color="text.secondary" variant="caption">
                {formatAggregate(bundle.aggregateImpact)}
              </Typography>
              <Typography color={strengthColor(bundle.strength)} variant="caption" sx={{ fontWeight: 600 }}>
                {strengthLabel(bundle.strength)}
              </Typography>
            </Stack>
          </Stack>
          <LinearProgress
            color={
              bundle.strength === 'supportive'
                ? 'success'
                : bundle.strength === 'headwind'
                  ? 'error'
                  : 'inherit'
            }
            sx={{
              bgcolor: 'action.hover',
              height: 4,
              borderRadius: 2,
              ...(bundle.strength === 'mixed' && {
                '& .MuiLinearProgress-bar': { bgcolor: 'text.secondary' },
              }),
            }}
            value={Math.min(100, Math.abs(bundle.aggregateImpact) * 8 + 18)}
            variant="determinate"
          />
          <Stack spacing={0.35}>
            {bundle.previewSignals.map((item) => (
              <Typography
                color="text.secondary"
                key={item.id}
                sx={{
                  display: 'block',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                }}
                variant="caption"
              >
                · {item.label}
              </Typography>
            ))}
            {hiddenCount > 0 ? (
              <Typography color="text.secondary" variant="caption">
                +{hiddenCount} more in detail
              </Typography>
            ) : null}
          </Stack>
        </Stack>
      </AccordionSummary>
      <AccordionDetails sx={{ borderTop: '1px solid', borderColor: 'divider', px: { xs: 1.5, sm: 2 }, py: 1.5 }}>
        <Stack spacing={1.5}>
          {bundle.signals.map((item) => (
            <SignalDetail item={item} key={item.id} />
          ))}
        </Stack>
      </AccordionDetails>
    </Accordion>
  )
}

function WatchoutsPanel({ watchouts }: { watchouts: Explanation[] }) {
  if (watchouts.length === 0) {
    return null
  }

  return (
    <Stack
      spacing={1}
      sx={{
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        p: { xs: 1.5, sm: 2 },
      }}
    >
      <Typography color="text.secondary" variant="overline" sx={{ letterSpacing: '0.08em' }}>
        Watchouts
      </Typography>
      <Stack spacing={1}>
        {watchouts.map((item) => (
          <Stack key={item.id} spacing={0.35}>
            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
              <Typography sx={{ fontWeight: 500 }} variant="body2">
                {item.label}
              </Typography>
              <Typography color={directionTone(item.direction)} variant="caption" sx={{ flexShrink: 0 }}>
                {item.direction === 'NEUTRAL' ? 'uncertain' : 'constraint'}
              </Typography>
            </Stack>
            <Typography color="text.secondary" variant="body2">
              {item.detail}
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Stack>
  )
}

export function ExplanationList({ explanations }: ExplanationListProps) {
  if (explanations.length === 0) {
    return (
      <Stack spacing={0.75}>
        <Typography variant="h3">Why this prediction</Typography>
        <Typography color="text.secondary" variant="body2">
          Contextual signals are not available for this prediction yet.
        </Typography>
      </Stack>
    )
  }

  const summary = buildPredictionSummary(explanations)
  const keyDrivers = getTopDrivers(explanations, 5)
  const categories = groupExplanationsByCategory(explanations)
  const watchouts = getWatchouts(explanations, 4)

  return (
    <Stack spacing={2.5}>
      <Stack spacing={0.75}>
        <Typography variant="h3">Why this prediction</Typography>
        <Typography color="text.secondary" variant="body2">
          {summary}
        </Typography>
      </Stack>

      <KeyDriversChart drivers={keyDrivers} />

      <Stack spacing={1.25}>
        <Typography color="text.secondary" variant="overline" sx={{ letterSpacing: '0.08em' }}>
          Signal groups
        </Typography>
        <Box
          sx={{
            display: 'grid',
            gap: 1.25,
            gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
          }}
        >
          {categories.map((bundle) => (
            <CategoryCard bundle={bundle} key={bundle.id} />
          ))}
        </Box>
      </Stack>

      <WatchoutsPanel watchouts={watchouts} />
    </Stack>
  )
}
