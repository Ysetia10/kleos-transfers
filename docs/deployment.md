# Production deployment (Vercel + Render + Supabase)

Free-tier stack for Kleos Transfers:

| Component | Host | Notes |
|-----------|------|--------|
| Frontend | **Vercel** | Static Vite build from `frontend/` — project `kleos-transfer` |
| Backend API | **Render** | Docker image from `backend/Dockerfile` |
| PostgreSQL | **Supabase** | Managed Postgres; Flyway on boot |
| Domain | `https://kleos-transfer.vercel.app` | Custom domain optional later |
| SSL | Included on Vercel + Render | — |

Day-2 ops (cold start, monitoring, rate limits, backups): **[`ops.md`](./ops.md)**.

## Architecture

```text
Browser
   ↓ HTTPS
Vercel (React SPA)
   ↓ VITE_API_BASE_URL
Render (Spring Boot)
   ↓ JDBC (Session pooler)
Supabase (PostgreSQL)
```

**Local development** uses `frontend/.env.local` with `localhost` (see `.env.local.example`).  
**Production** uses `frontend/vercel.json` `build.env` (+ optional Vercel dashboard) — **never** create `frontend/.env` with localhost (Vercel CLI can bake it into the prod bundle).

---

## 1. Supabase (database)

1. Create a project on [Supabase](https://supabase.com).
2. **Settings → Database** → copy connection details.
3. Prefer the **Session pooler** JDBC URL for Render (IPv4):

```text
jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
```

Username: `postgres.<project-ref>` (not bare `postgres`).

4. Set on **Render** (not in git):

| Variable | Example |
|----------|---------|
| `DATABASE_URL` | `jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | `postgres.<project-ref>` |
| `DATABASE_PASSWORD` | *(from Supabase)* |

> **Important:** The direct host `db.<ref>.supabase.co` is often **IPv6-only**. Render cannot reach it (`Network is unreachable`). Use **Session pooler** (port 5432).

5. **Seed data:** export local DB and restore to Supabase (`scripts/complete-supabase.sh`), or re-run ingest scripts against prod after deploy.

```bash
pg_dump -h localhost -U kleos -d kleos_transfers --no-owner --no-acl -Fc -f kleos.dump
# restore via pg_restore to pooler/direct host — see ops.md backups
```

Flyway runs on backend startup in **local/dev** (`ddl-auto: validate`). In **prod**, Flyway is off by default (`SPRING_FLYWAY_ENABLED=false`) for faster cold starts — enable for one deploy when applying new migrations (see [`ops.md`](./ops.md)).

---

## 2. Render (backend API)

### Option A — Blueprint

1. Push this repo to GitHub.
2. Render → **New → Blueprint** → connect repo (`render.yaml` at root).
3. Set secret env vars when prompted:
   - `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
   - `CORS_ALLOWED_ORIGINS` → exact Vercel origin, e.g. `https://kleos-transfer.vercel.app`
4. Deploy. Service URL: `https://kleos-transfers-api.onrender.com`.

### Option B — Manual web service

- **Runtime:** Docker
- **Dockerfile path:** `backend/Dockerfile`
- **Docker context:** repository root
- **Health check path:** `/api/v1/health`
- **Environment:** `SPRING_PROFILES_ACTIVE=prod` plus DB + CORS vars above

### Verify

```bash
curl https://kleos-transfers-api.onrender.com/api/v1/health
curl https://kleos-transfers-api.onrender.com/actuator/health
```

Free tier may sleep after inactivity (cold start ~30–90s after optimizations) unless **Keep warm** workflow is running — see [`ops.md`](./ops.md).

---

## 3. Vercel (frontend)

1. Import GitHub repo on [Vercel](https://vercel.com).
2. **Root Directory:** `frontend`
3. **Project name:** `kleos-transfer` → `https://kleos-transfer.vercel.app`
4. **Framework Preset:** Vite (auto)
5. **Environment variables** (Production) — optional backup; `vercel.json` already sets:

| Variable | Value |
|----------|--------|
| `VITE_API_BASE_URL` | `https://kleos-transfers-api.onrender.com` |

No trailing slash. **Do not** use `localhost` in production.

6. Deploy. URL: https://kleos-transfer.vercel.app

`frontend/vercel.json` handles SPA routing and production `VITE_API_BASE_URL` when deploying from the `frontend/` directory (CLI).

For **Git-connected** deploys, the repository root `vercel.json` builds `frontend/dist` so backend-only pushes do not produce empty production deployments. In the Vercel dashboard, set **Root Directory** to the **repository root** (leave blank / `.`), not `frontend`.

### Local frontend

```bash
cd frontend
cp .env.local.example .env.local
# VITE_API_BASE_URL=http://localhost:8080
npm run dev
```

---

## 4. CORS checklist

Render `CORS_ALLOWED_ORIGINS` must include **exact** browser origins:

```text
https://kleos-transfer.vercel.app
```

Add preview URLs if needed (comma-separated, no spaces).  
Local dev defaults remain `http://localhost:5173,http://127.0.0.1:5173` when unset.

After changing CORS, redeploy Render.

---

## 5. Environment variable reference

### Frontend (Vite)

| Variable | Local | Production |
|----------|-------|------------|
| `VITE_API_BASE_URL` | `http://localhost:8080` via `.env.local` | `https://kleos-transfers-api.onrender.com` via `vercel.json` |

This project uses **Vite**, not Next.js — use `VITE_API_BASE_URL` (not `NEXT_PUBLIC_*`).

### Backend (Spring Boot)

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | Yes | JDBC PostgreSQL URL (pooler on Render) |
| `DATABASE_USERNAME` | Yes | DB user (`postgres.<ref>` on pooler) |
| `DATABASE_PASSWORD` | Yes | DB password |
| `CORS_ALLOWED_ORIGINS` | Yes (prod) | Comma-separated UI origins |
| `SPRING_PROFILES_ACTIVE` | Recommended | `prod` on Render |
| `PORT` | Auto on Render | HTTP port (Render sets this) |
| `KLEOS_RATE_LIMIT_ENABLED` | Optional | Default `true` |
| `KLEOS_RATE_LIMIT_RPM` | Optional | Default `60` requests/min/IP |

---

## 6. CI vs production

GitHub Actions builds the frontend with the **production** API URL and fails if `dist/assets` contains `localhost:8080`.  
Scheduled **Keep warm** (every 8 min) and **Production ops** (hourly smoke) workflows keep the live API warm and monitored.

---

## Related issues

- Closed [#75](https://github.com/Ysetia10/kleos-transfers/issues/75)–[#77](https://github.com/Ysetia10/kleos-transfers/issues/77) go-live
- [#78](https://github.com/Ysetia10/kleos-transfers/issues/78) cold start · [#82](https://github.com/Ysetia10/kleos-transfers/issues/82) monitoring · [#84](https://github.com/Ysetia10/kleos-transfers/issues/84) localhost guard
- [#85](https://github.com/Ysetia10/kleos-transfers/issues/85) backups · [#86](https://github.com/Ysetia10/kleos-transfers/issues/86) rate limits · [#87](https://github.com/Ysetia10/kleos-transfers/issues/87) deploy commit
