# Frontend

Vite-powered React 19 and TypeScript app for Kleos Transfers, using Material UI as its UI library.

## Included

- Light/dark Material UI themes with a persisted toggle
- Shared route constants for routing and navigation
- TanStack Query + Axios client with `ApiError` normalization
- Typed API clients for players, clubs, seasons, stats, and predictions
- Product pages: Simulator, Players, Clubs, Trending, Methodology, prediction results
- React Hook Form + Zod validation on the prediction form
- Explanation list with optional Recharts factor chart

## Commands

```bash
npm install
npm run dev
npm run build
npm run lint
```

Copy `.env.example` to `.env` and set:

```bash
VITE_API_BASE_URL=http://localhost:8080
```

Open the app at `http://localhost:5173` or `http://127.0.0.1:5173` — both are allowed by the API CORS defaults.

Backend routes are versioned under `/api/v1`. The prediction form needs players, clubs, and seasons already loaded in the API.
