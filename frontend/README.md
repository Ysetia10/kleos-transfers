# Frontend

Vite-powered React 19 and TypeScript app for Kleos Transfers, using Material UI as its UI library.

## Included

- Light/dark Material UI themes with a persisted toggle
- Shared route constants for routing and navigation
- TanStack Query + Axios client with `ApiError` normalization
- Typed API clients for players, clubs, seasons, stats, and predictions
- Product pages: Prediction, Players, Clubs, Transfers, Trending, prediction results
- React Hook Form + Zod validation on the prediction form
- Explanation list with optional Recharts factor chart

## Commands

```bash
npm install
npm run dev
npm run build
npm run lint
```

Copy `.env.example` to `.env.local` for local development:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

**Production (Vercel):** set `VITE_API_BASE_URL` to your Render API URL in the Vercel dashboard — see `../docs/deployment.md`. Do not commit production URLs or use `localhost` in deployed builds.

Open the app at `http://localhost:5173` or `http://127.0.0.1:5173` — both are allowed by the API CORS defaults.

Backend routes are versioned under `/api/v1`. The prediction form needs players, clubs, and seasons already loaded in the API.
