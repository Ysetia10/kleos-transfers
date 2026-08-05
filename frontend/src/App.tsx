import { Route, Routes } from 'react-router-dom'
import { routes } from '@/constants/routes'
import { MainLayout } from '@/layouts/MainLayout'
import { AboutPage } from '@/pages/AboutPage'
import { ClubsPage } from '@/pages/ClubsPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlayersPage } from '@/pages/PlayersPage'
import { PredictionPage } from '@/pages/PredictionPage'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path={routes.prediction.slice(1)} element={<PredictionPage />} />
        <Route path={routes.players.slice(1)} element={<PlayersPage />} />
        <Route path={routes.clubs.slice(1)} element={<ClubsPage />} />
        <Route path={routes.dashboard.slice(1)} element={<DashboardPage />} />
        <Route path={routes.about.slice(1)} element={<AboutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
