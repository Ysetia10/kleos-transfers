import { Navigate, useSearchParams } from 'react-router-dom'
import type { HomeSectionId } from '@/constants/routes'
import { routes } from '@/constants/routes'

interface LegacyHomeRedirectProps {
  section: HomeSectionId
}

/** Keep old top-level paths working by sending them into Home sections. */
export function LegacyHomeRedirect({ section }: LegacyHomeRedirectProps) {
  const [params] = useSearchParams()
  const search = params.toString()

  return (
    <Navigate
      replace
      to={{
        pathname: routes.home,
        search: search ? `?${search}` : '',
        hash: `#${section}`,
      }}
    />
  )
}
