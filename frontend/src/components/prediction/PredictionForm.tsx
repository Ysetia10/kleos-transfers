import { zodResolver } from '@hookform/resolvers/zod'
import { Alert, Autocomplete, Button, Stack, TextField } from '@mui/material'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'
import { routes } from '@/constants/routes'
import { queryKeys } from '@/services/api/queryKeys'
import { listClubs } from '@/services/club/clubApi'
import { createPrediction } from '@/services/prediction/predictionApi'
import { listPlayers } from '@/services/player/playerApi'
import { listSeasons } from '@/services/season/seasonApi'
import { ApiError } from '@/types/api'
import type { Club, Player, Season } from '@/types/domain'

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
}

export function PredictionForm({ initialPlayerId, initialClubId }: PredictionFormProps) {
  const navigate = useNavigate()

  const playersQuery = useQuery({
    queryKey: queryKeys.players.list(0, 100),
    queryFn: () => listPlayers(0, 100),
  })
  const clubsQuery = useQuery({
    queryKey: queryKeys.clubs.list(0, 100),
    queryFn: () => listClubs(0, 100),
  })
  const seasonsQuery = useQuery({
    queryKey: queryKeys.seasons.list(0, 50),
    queryFn: () => listSeasons(0, 50),
  })

  const players = playersQuery.data?.content ?? []
  const clubs = clubsQuery.data?.content ?? []
  const seasons = seasonsQuery.data?.content ?? []

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

  const mutation = useMutation({
    mutationFn: createPrediction,
    onSuccess: (prediction) => {
      void navigate(routes.predictionDetail(prediction.id))
    },
  })

  if (playersQuery.isLoading || clubsQuery.isLoading || seasonsQuery.isLoading) {
    return <LoadingState label="Loading form options…" />
  }

  if (playersQuery.isError || clubsQuery.isError || seasonsQuery.isError) {
    return (
      <ErrorState
        error={playersQuery.error ?? clubsQuery.error ?? seasonsQuery.error}
        onRetry={() => {
          void playersQuery.refetch()
          void clubsQuery.refetch()
          void seasonsQuery.refetch()
        }}
      />
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
      sx={{ maxWidth: 640 }}
    >
      <Controller
        control={control}
        name="playerId"
        render={({ field }) => (
          <Autocomplete<Player>
            getOptionLabel={(option) =>
              `${option.fullName} · ${option.primaryPosition} · ${option.nationality}`
            }
            isOptionEqualToValue={(option, value) => option.id === value.id}
            onChange={(_, value) => field.onChange(value?.id ?? '')}
            options={players}
            renderInput={(params) => (
              <TextField
                {...params}
                error={!!errors.playerId}
                helperText={errors.playerId?.message}
                label="Player"
                required
              />
            )}
            value={players.find((player) => player.id === field.value) ?? null}
          />
        )}
      />

      <Controller
        control={control}
        name="targetClubId"
        render={({ field }) => (
          <Autocomplete<Club>
            getOptionLabel={(option) => `${option.name} (${option.countryCode})`}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            onChange={(_, value) => field.onChange(value?.id ?? '')}
            options={clubs}
            renderInput={(params) => (
              <TextField
                {...params}
                error={!!errors.targetClubId}
                helperText={errors.targetClubId?.message}
                label="Target club"
                required
              />
            )}
            value={clubs.find((club) => club.id === field.value) ?? null}
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

      {mutation.isError ? (
        <Alert severity="error" variant="outlined">
          {mutation.error instanceof ApiError
            ? mutation.error.message
            : 'Prediction request failed'}
        </Alert>
      ) : null}

      <Button disabled={mutation.isPending} type="submit" variant="contained">
        {mutation.isPending ? 'Running prediction…' : 'Run prediction'}
      </Button>
    </Stack>
  )
}
