import { Alert, AlertTitle, Button, Stack } from '@mui/material'
import { ApiError } from '@/types/api'
import { userFacingErrorMessage } from '@/utils/userFacingError'

interface ErrorStateProps {
  error: unknown
  onRetry?: () => void
}

export function ErrorState({ error, onRetry }: ErrorStateProps) {
  const message = userFacingErrorMessage(error)

  return (
    <Alert
      action={
        onRetry ? (
          <Button color="inherit" onClick={onRetry} size="small">
            Try again
          </Button>
        ) : undefined
      }
      severity="error"
      variant="outlined"
    >
      <AlertTitle>Something went wrong</AlertTitle>
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
