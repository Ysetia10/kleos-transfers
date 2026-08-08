import { useEffect, useState } from 'react'

/** Debounce a changing value; useful for search boxes. */
export function useDebouncedValue<T>(value: T, delayMs = 250): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const handle = window.setTimeout(() => setDebounced(value), delayMs)
    return () => window.clearTimeout(handle)
  }, [value, delayMs])
  return debounced
}
