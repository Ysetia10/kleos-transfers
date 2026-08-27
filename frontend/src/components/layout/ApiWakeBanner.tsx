import { Alert, Collapse } from '@mui/material'
import { useEffect, useState } from 'react'
import { subscribeApiWaking } from '@/services/api/apiWake'

/**
 * Shown while the Render free-tier API is waking from sleep (often 30–90s when warm-path misses).
 */
export function ApiWakeBanner() {
  const [waking, setWaking] = useState(false)

  useEffect(() => subscribeApiWaking(setWaking), [])

  return (
    <Collapse in={waking}>
      <Alert severity="info" sx={{ borderRadius: 0 }} variant="filled">
        Starting the API — first load after idle can take up to a couple of minutes.
      </Alert>
    </Collapse>
  )
}
