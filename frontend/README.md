# Frontend

Vite-powered React 19 and TypeScript foundation for Kleos Transfers, using Material UI as its only UI component library.

## Included foundation

- Centralized light and dark Material UI themes with semantic success/warning/error colors
- Shared route constants used by routing and navigation
- TanStack Query provider, Axios client with API error normalization, and query-key helpers
- Empty product pages ready for Player / Club / Prediction features

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

Backend routes are versioned under `/api/v1`.
