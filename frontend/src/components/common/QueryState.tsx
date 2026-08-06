import type { ReactNode } from 'react'
import { EmptyState } from '@/components/common/EmptyState'
import { ErrorState } from '@/components/common/ErrorState'
import { LoadingState } from '@/components/common/LoadingState'

interface QueryStateProps {
  isLoading: boolean
  isError: boolean
  error: unknown
  isEmpty: boolean
  emptyTitle: string
  emptyDescription?: string
  onRetry?: () => void
  children: ReactNode
}

export function QueryState({
  isLoading,
  isError,
  error,
  isEmpty,
  emptyTitle,
  emptyDescription,
  onRetry,
  children,
}: QueryStateProps) {
  if (isLoading) {
    return <LoadingState />
  }
  if (isError) {
    return <ErrorState error={error} onRetry={onRetry} />
  }
  if (isEmpty) {
    return <EmptyState description={emptyDescription} title={emptyTitle} />
  }
  return children
}
