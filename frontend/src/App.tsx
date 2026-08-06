import { Route, Routes } from 'react-router-dom'
import { routes } from '@/constants/routes'
import { MainLayout } from '@/layouts/MainLayout'
import { AboutPage } from '@/pages/AboutPage'
import { ClubDetailPage } from '@/pages/ClubDetailPage'
import { ClubsPage } from '@/pages/ClubsPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlayerDetailPage } from '@/pages/PlayerDetailPage'
import { PlayersPage } from '@/pages/PlayersPage'
import { PredictionPage } from '@/pages/PredictionPage'
import { PredictionResultPage } from '@/pages/PredictionResultPage'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path={routes.prediction.slice(1)} element={<PredictionPage />} />
        <Route path="predictions/:id" element={<PredictionResultPage />} />
        <Route path={routes.players.slice(1)} element={<PlayersPage />} />
        <Route path="players/:id" element={<PlayerDetailPage />} />
        <Route path={routes.clubs.slice(1)} element={<ClubsPage />} />
        <Route path="clubs/:id" element={<ClubDetailPage />} />
        <Route path={routes.dashboard.slice(1)} element={<DashboardPage />} />
        <Route path={routes.about.slice(1)} element={<AboutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
