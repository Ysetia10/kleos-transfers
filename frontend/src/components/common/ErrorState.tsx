import { Alert, AlertTitle, Button, Stack } from '@mui/material'
import { ApiError } from '@/types/api'

interface ErrorStateProps {
  error: unknown
  onRetry?: () => void
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  const message =
    error instanceof ApiError
      ? error.message
      : error instanceof Error
        ? error.message
        : 'Something went wrong'

  return (
    <Alert
      action={
        onRetry ? (
          <Button color="inherit" onClick={onRetry} size="small">
            Retry
          </Button>
        ) : undefined
      }
      severity="error"
      variant="outlined"
    >
      <AlertTitle>Unable to load</AlertTitle>
      <Stack spacing={0.5}>
        <span>{message}</span>
        {error instanceof ApiError && error.violations.length > 0
          ? error.violations.map((violation) => (
              <span key={`${violation.field}-${violation.message}`}>
                {violation.field}: {violation.message}
              </span>
            ))
          : null}
      </Stack>
    </Alert>
  )
}
