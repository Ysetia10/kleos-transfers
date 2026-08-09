import { Navigate, Route, Routes } from 'react-router-dom'
import { routes } from '@/constants/routes'
import { MainLayout } from '@/layouts/MainLayout'
import { ClubDetailPage } from '@/pages/ClubDetailPage'
import { ClubsPage } from '@/pages/ClubsPage'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { PlayerDetailPage } from '@/pages/PlayerDetailPage'
import { PlayersPage } from '@/pages/PlayersPage'
import { PredictionResultPage } from '@/pages/PredictionResultPage'
import { TransfersPage } from '@/pages/TransfersPage'
import { TrendingPage } from '@/pages/TrendingPage'

export default function App() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route index element={<HomePage />} />
        <Route path={routes.prediction.slice(1)} element={<Navigate replace to={routes.home} />} />
        <Route path="predictions/:id" element={<PredictionResultPage />} />
        <Route path={routes.players.slice(1)} element={<PlayersPage />} />
        <Route path="players/:id" element={<PlayerDetailPage />} />
        <Route path={routes.clubs.slice(1)} element={<ClubsPage />} />
        <Route path="clubs/:id" element={<ClubDetailPage />} />
        <Route path={routes.transfers.slice(1)} element={<TransfersPage />} />
        <Route path={routes.trending.slice(1)} element={<TrendingPage />} />
        <Route
          path={routes.dashboard.slice(1)}
          element={<Navigate replace to={routes.trending} />}
        />
        <Route path="methodology" element={<Navigate replace to={routes.home} />} />
        <Route path="about" element={<Navigate replace to={routes.home} />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
