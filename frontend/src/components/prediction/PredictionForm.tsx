import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Autocomplete, Box, Button, Stack, TextField, Typography } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { SquadTable } from '@/components/home/SquadTable'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { getClub, listClubs } from '@/services/club/clubApi'
import { getClubSquad } from '@/services/club/squadApi'
import { createPrediction } from '@/services/prediction/predictionApi'
import { getPlayer, listPlayers } from '@/services/player/playerApi'
import { listSeasons } from '@/services/season/seasonApi'
import { ApiError } from '@/types/api'
import type { Club, Player, Season } from '@/types/domain'
import { formatFootballCountry } from '@/utils/footballCountry'

const schema = z.object({
  playerId: z.string().uuid('Select a player'),
  targetClubId: z.string().uuid('Select a target club'),
  seasonId: z.string().uuid('Select a season'),
  note: z.string().max(255).optional(),
})

type FormValues = z.infer<typeof schema>

interface PredictionFormProps {
  initialPlayerId?: string
  initialClubId?: string
  showSquad?: boolean
}

const SEARCH_PAGE_SIZE = 25

function useDebouncedSearch(value: string, delayMs = 250) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const handle = window.setTimeout(() => setDebounced(value), delayMs)
    return () => window.clearTimeout(handle)
  }, [value, delayMs])
  return debounced
}

export function PredictionForm({
  initialPlayerId,
  initialClubId,
  showSquad = false,
}: PredictionFormProps) {
  const navigate = useNavigate()
  const [playerInput, setPlayerInput] = useState('')
  const [clubInput, setClubInput] = useState('')
  const debouncedPlayerQuery = useDebouncedSearch(playerInput)
  const debouncedClubQuery = useDebouncedSearch(clubInput)

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

  const seasons = seasonsQuery.data?.content ?? []
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
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      playerId: initialPlayerId ?? '',
      targetClubId: initialClubId ?? '',
      seasonId: '',
      note: '',
    },
  })

  const watchedClubId = useWatch({ control, name: 'targetClubId' })
  const watchedSeasonId = useWatch({ control, name: 'seasonId' })
  const squadEnabled = showSquad && !!watchedClubId && !!watchedSeasonId

  const squadQuery = useQuery({
    queryKey: queryKeys.clubs.squad(watchedClubId || '', watchedSeasonId || ''),
    queryFn: () => getClubSquad(watchedClubId, watchedSeasonId),
    enabled: squadEnabled,
  })

  const selectedSeasonLabel =
    seasons.find((season) => season.id === watchedSeasonId)?.label ?? watchedSeasonId
  const selectedClubName =
    clubOptions.find((club) => club.id === watchedClubId)?.name ?? 'Selected club'

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
          note: values.note?.trim() || undefined,
        })
      })}
      spacing={3}
    >
      <Box
        sx={{
          display: 'grid',
          gap: 3,
          gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
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
                  helperText={errors.playerId?.message ?? 'Type a name to search all players'}
                  label="Player"
                  required
                />
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
                  helperText={errors.targetClubId?.message ?? 'Type a name to search all clubs'}
                  label="Target club"
                  required
                />
              )}
              value={clubOptions.find((club) => club.id === field.value) ?? null}
            />
          )}
        />

        <Controller
          control={control}
          name="seasonId"
          render={({ field }) => (
            <Autocomplete<Season>
              getOptionLabel={(option) => option.label}
              isOptionEqualToValue={(option, value) => option.id === value.id}
              onChange={(_, value) => field.onChange(value?.id ?? '')}
              options={seasons}
              renderInput={(params) => (
                <TextField
                  {...params}
                  error={!!errors.seasonId}
                  helperText={errors.seasonId?.message}
                  label="Season"
                  required
                />
              )}
              value={seasons.find((season) => season.id === field.value) ?? null}
            />
          )}
        />

        <Controller
          control={control}
          name="note"
          render={({ field }) => (
            <TextField
              {...field}
              error={!!errors.note}
              helperText={errors.note?.message}
              label="Note (optional)"
              multiline
              rows={2}
            />
          )}
        />
      </Box>

      {mutation.isError ? (
        <Alert severity="error" variant="outlined">
          {mutation.error instanceof ApiError
            ? mutation.error.message
            : 'Prediction request failed'}
        </Alert>
      ) : null}

      <Button
        disabled={mutation.isPending}
        sx={{ alignSelf: 'flex-start' }}
        type="submit"
        variant="contained"
      >
        {mutation.isPending ? 'Running prediction…' : 'Run prediction'}
      </Button>

      {showSquad ? (
        <Stack spacing={1.5}>
          <Typography component="h3" variant="h3">
            Target club squad
          </Typography>
          <Typography color="text.secondary" variant="body2">
            {squadEnabled
              ? `${selectedClubName} · ${selectedSeasonLabel} — full season roster from PlayerSeason rows.`
              : 'Select a target club and season to load the squad for that campaign.'}
          </Typography>
          {squadEnabled ? (
            <SquadTable
              error={squadQuery.error}
              isError={squadQuery.isError}
              isLoading={squadQuery.isLoading}
              onRetry={() => void squadQuery.refetch()}
              squad={squadQuery.data}
            />
          ) : null}
        </Stack>
      ) : null}
    </Stack>
  )
}
