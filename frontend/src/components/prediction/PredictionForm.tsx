import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Autocomplete, Box, Button, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ErrorState } from '@/components/common/ErrorState'
import { IdentityMedia } from '@/components/common/IdentityMedia'
import { LoadingState } from '@/components/common/LoadingState'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClub, listClubs } from '@/services/club/clubApi'
import { createPrediction } from '@/services/prediction/predictionApi'
import { getPlayer, listPlayers } from '@/services/player/playerApi'
import { listSeasons } from '@/services/season/seasonApi'
import { userFacingErrorMessage } from '@/utils/userFacingError'
import type { Club, Player, Season } from '@/types/domain'
import { formatFootballCountry } from '@/utils/footballCountry'
import { useDebouncedValue } from '@/utils/useDebouncedValue'

const schema = z.object({
  playerId: z.string().uuid('Select a player'),
  targetClubId: z.string().uuid('Select a target club'),
  seasonId: z.string().uuid('Select a season'),
})

type FormValues = z.infer<typeof schema>

interface PredictionFormProps {
  initialPlayerId?: string
  initialClubId?: string
  /** upcoming = current campaign only; historical = completed seasons (backtest path). */
  seasonMode?: 'upcoming' | 'historical'
}

const SEARCH_PAGE_SIZE = 25

function isUpcomingSeason(season: Season, today = new Date()): boolean {
  const end = new Date(`${season.endDate}T23:59:59`)
  return end >= today
}

function seasonOptionLabel(season: Season): string {
  return isUpcomingSeason(season) ? `${season.label} · upcoming` : season.label
}

export function PredictionForm({
  initialPlayerId,
  initialClubId,
  seasonMode = 'upcoming',
}: PredictionFormProps) {
  const navigate = useNavigate()
  const [playerInput, setPlayerInput] = useState('')
  const [clubInput, setClubInput] = useState('')
  const debouncedPlayerQuery = useDebouncedValue(playerInput)
  const debouncedClubQuery = useDebouncedValue(clubInput)

  const seasonsQuery = useQuery({
    queryKey: queryKeys.seasons.list(0, 50),
    queryFn: () => listSeasons(0, 50),
  })

  const playersQuery = useQuery({
    queryKey: queryKeys.players.list(0, SEARCH_PAGE_SIZE, debouncedPlayerQuery),
    queryFn: () => listPlayers(0, SEARCH_PAGE_SIZE, debouncedPlayerQuery || undefined),
  })

  const clubsQuery = useQuery({
    queryKey: queryKeys.clubs.list(0, SEARCH_PAGE_SIZE, debouncedClubQuery),
    queryFn: () => listClubs(0, SEARCH_PAGE_SIZE, debouncedClubQuery || undefined),
  })

  const initialPlayerQuery = useQuery({
    queryKey: queryKeys.players.detail(initialPlayerId ?? ''),
    queryFn: () => getPlayer(initialPlayerId!),
    enabled: !!initialPlayerId,
  })

  const initialClubQuery = useQuery({
    queryKey: queryKeys.clubs.detail(initialClubId ?? ''),
    queryFn: () => getClub(initialClubId!),
    enabled: !!initialClubId,
  })

  const allSeasons = seasonsQuery.data?.content ?? []
  const seasons = useMemo(() => {
    if (seasonMode === 'historical') {
      return allSeasons.filter((season) => !isUpcomingSeason(season))
    }
    const upcoming = allSeasons.filter((season) => isUpcomingSeason(season))
    return upcoming.length > 0 ? upcoming : allSeasons.slice(0, 1)
  }, [allSeasons, seasonMode])

  const playerOptions = useMemo(() => {
    const byId = new Map<string, Player>()
    for (const player of playersQuery.data?.content ?? []) {
      byId.set(player.id, player)
    }
    if (initialPlayerQuery.data) {
      byId.set(initialPlayerQuery.data.id, initialPlayerQuery.data)
    }
    return [...byId.values()]
  }, [playersQuery.data?.content, initialPlayerQuery.data])

  const clubOptions = useMemo(() => {
    const byId = new Map<string, Club>()
    for (const club of clubsQuery.data?.content ?? []) {
      byId.set(club.id, club)
    }
    if (initialClubQuery.data) {
      byId.set(initialClubQuery.data.id, initialClubQuery.data)
    }
    return [...byId.values()]
  }, [clubsQuery.data?.content, initialClubQuery.data])

  const {
    control,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      playerId: initialPlayerId ?? '',
      targetClubId: initialClubId ?? '',
      seasonId: '',
    },
  })

  const watchedSeasonId = useWatch({ control, name: 'seasonId' })

  // Lock to the newest eligible season for the active mode when unset or out of scope.
  useEffect(() => {
    if (!seasons.length) {
      return
    }
    const stillValid = seasons.some((season) => season.id === watchedSeasonId)
    if (!watchedSeasonId || !stillValid) {
      setValue('seasonId', seasons[0].id)
    }
  }, [seasons, setValue, watchedSeasonId])

  const selectedSeason = seasons.find((season) => season.id === watchedSeasonId) ?? null
  const priorSeason = useMemo(() => {
    if (!selectedSeason) {
      return null
    }
    return (
      allSeasons
        .filter((season) => season.startDate < selectedSeason.startDate)
        .sort((a, b) => b.startDate.localeCompare(a.startDate))[0] ?? null
    )
  }, [allSeasons, selectedSeason])

  const upcomingSelected = selectedSeason ? isUpcomingSeason(selectedSeason) : false

  const mutation = useMutation({
    mutationFn: createPrediction,
    onSuccess: (prediction) => {
      void navigate(routes.predictionDetail(prediction.id))
    },
  })

  if (seasonsQuery.isLoading) {
    return <LoadingState label="Loading form options…" />
  }

  if (seasonsQuery.isError) {
    return (
      <ErrorState error={seasonsQuery.error} onRetry={() => void seasonsQuery.refetch()} />
    )
  }

  return (
    <Stack
      component="form"
      noValidate
      onSubmit={handleSubmit((values) => {
        mutation.mutate({
          playerId: values.playerId,
          targetClubId: values.targetClubId,
          seasonId: values.seasonId,
        })
      })}
      spacing={3}
    >
      <Box
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: {
            xs: '1fr',
            md: 'minmax(0, 1.2fr) minmax(0, 1.1fr) minmax(0, 0.9fr) auto',
          },
          alignItems: 'start',
        }}
      >
        <Controller
          control={control}
          name="playerId"
          render={({ field }) => (
            <Autocomplete<Player>
              filterOptions={(options) => options}
              getOptionLabel={(option) => {
                const club = option.latestClubName ? ` · ${option.latestClubName}` : ''
                const age = option.age != null ? ` · ${option.age}` : ''
                return `${option.fullName} · ${option.primaryPosition}${age}${club}`
              }}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              loading={playersQuery.isFetching}
              onChange={(_, value) => field.onChange(value?.id ?? '')}
              onInputChange={(_, value, reason) => {
                if (reason === 'input' || reason === 'clear') {
                  setPlayerInput(value)
                }
              }}
              options={playerOptions}
              renderInput={(params) => (
                <TextField
                  {...params}
                  error={!!errors.playerId}
                  helperText={errors.playerId?.message ?? 'Search all players'}
                  label="Player"
                  required
                />
              )}
              renderOption={(props, option) => (
                <Box component="li" {...props} key={option.id} sx={{ display: 'flex', gap: 1.25 }}>
                  <IdentityMedia imageUrl={option.photoUrl} label={option.fullName} size={28} />
                  <Box sx={{ minWidth: 0 }}>
                    <Typography noWrap variant="body2">
                      {option.fullName}
                    </Typography>
                    <Typography color="text.secondary" noWrap variant="caption">
                      {option.primaryPosition}
                      {option.latestClubName ? ` · ${option.latestClubName}` : ''}
                    </Typography>
                  </Box>
                </Box>
              )}
              value={playerOptions.find((player) => player.id === field.value) ?? null}
            />
          )}
        />

        <Controller
          control={control}
          name="targetClubId"
          render={({ field }) => (
            <Autocomplete<Club>
              filterOptions={(options) => options}
              getOptionLabel={(option) =>
                `${option.name} (${formatFootballCountry(option.countryCode)})`
              }
              isOptionEqualToValue={(option, value) => option.id === value.id}
              loading={clubsQuery.isFetching}
              onChange={(_, value) => field.onChange(value?.id ?? '')}
              onInputChange={(_, value, reason) => {
                if (reason === 'input' || reason === 'clear') {
                  setClubInput(value)
                }
              }}
              options={clubOptions}
              renderInput={(params) => (
                <TextField
                  {...params}
                  error={!!errors.targetClubId}
                  helperText={errors.targetClubId?.message ?? 'Search all clubs'}
                  label="Target club"
                  required
                />
              )}
              renderOption={(props, option) => (
                <Box component="li" {...props} key={option.id} sx={{ display: 'flex', gap: 1.25 }}>
                  <IdentityMedia
                    imageUrl={option.crestUrl}
                    label={option.name}
                    rounded="soft"
                    size={28}
                  />
                  <Box sx={{ minWidth: 0 }}>
                    <Typography noWrap variant="body2">
                      {option.name}
                    </Typography>
                    <Typography color="text.secondary" noWrap variant="caption">
                      {formatFootballCountry(option.countryCode)}
                    </Typography>
                  </Box>
                </Box>
              )}
              value={clubOptions.find((club) => club.id === field.value) ?? null}
            />
          )}
        />

        {seasonMode === 'historical' ? (
          <Controller
            control={control}
            name="seasonId"
            render={({ field }) => (
              <Autocomplete<Season>
                getOptionLabel={(option) => seasonOptionLabel(option)}
                isOptionEqualToValue={(option, value) => option.id === value.id}
                onChange={(_, value) => field.onChange(value?.id ?? '')}
                options={seasons}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    error={!!errors.seasonId}
                    helperText={
                      errors.seasonId?.message ??
                      'Completed seasons can be checked against actual outcomes'
                    }
                    label="Target season"
                    required
                  />
                )}
                value={seasons.find((season) => season.id === field.value) ?? null}
              />
            )}
          />
        ) : (
          <TextField
            helperText="Current campaign only — use previous-seasons mode for backtests"
            label="Target season"
            slotProps={{ input: { readOnly: true } }}
            value={selectedSeason ? seasonOptionLabel(selectedSeason) : '—'}
          />
        )}

        <Button
          disabled={mutation.isPending}
          sx={{ height: 56, px: 3, whiteSpace: 'nowrap' }}
          type="submit"
          variant="contained"
        >
          {mutation.isPending ? 'Running…' : 'Run prediction'}
        </Button>
      </Box>

      {seasonMode === 'upcoming' && upcomingSelected ? (
        <Alert severity="info" variant="outlined">
          Predicting <strong>{selectedSeason?.label}</strong> before kick-off. Destination squad starts
          from the club’s <strong>latest completed roster</strong>
          {priorSeason ? <> ({priorSeason.label})</> : null}, then removes confirmed outs and adds
          confirmed/announced ins for {selectedSeason?.label}.
        </Alert>
      ) : null}

      {seasonMode === 'historical' ? (
        <Alert severity="info" variant="outlined">
          Previous-season mode projects into a completed campaign so you can compare against actual
          minutes and output when PlayerSeason rows exist.
        </Alert>
      ) : null}

      {mutation.isError ? (
        <Alert severity="error" variant="outlined">
          {userFacingErrorMessage(mutation.error)}
        </Alert>
      ) : null}
    </Stack>
  )
}
