import { Route, Routes } from 'react-router-dom'
import { homeSections, routes } from '@/constants/routes'
import { MainLayout } from '@/layouts/MainLayout'
import { AboutPage } from '@/pages/AboutPage'
import { ClubDetailPage } from '@/pages/ClubDetailPage'
import { HomePage } from '@/pages/HomePage'
import { LegacyHomeRedirect } from '@/pages/LegacyHomeRedirect'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlayerDetailPage } from '@/pages/PlayerDetailPage'
import { PredictionResultPage } from '@/pages/PredictionResultPage'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route
          path={routes.prediction.slice(1)}
          element={<LegacyHomeRedirect section={homeSections.predict} />}
        />
        <Route path="predictions/:id" element={<PredictionResultPage />} />
        <Route
          path={routes.players.slice(1)}
          element={<LegacyHomeRedirect section={homeSections.catalogue} />}
        />
        <Route path="players/:id" element={<PlayerDetailPage />} />
        <Route
          path={routes.clubs.slice(1)}
          element={<LegacyHomeRedirect section={homeSections.catalogue} />}
        />
        <Route path="clubs/:id" element={<ClubDetailPage />} />
        <Route
          path={routes.dashboard.slice(1)}
          element={<LegacyHomeRedirect section={homeSections.recent} />}
        />
        <Route path={routes.about.slice(1)} element={<AboutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
