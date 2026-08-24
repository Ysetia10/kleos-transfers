# Production deployment (Vercel + Render + Supabase)

Free-tier stack for Kleos Transfers:

| Component | Host | Notes |
|-----------|------|--------|
| Frontend | **Vercel** | Static Vite build from `frontend/` |
| Backend API | **Render** | Docker image from `backend/Dockerfile` |
| PostgreSQL | **Supabase** | Managed Postgres; Flyway on boot |
| Domain | `*.vercel.app` initially | Custom domain optional later |
| SSL | Included on Vercel + Render | — |

## Architecture

```text
Browser
   ↓ HTTPS
Vercel (React SPA)
   ↓ VITE_API_BASE_URL
Render (Spring Boot)
   ↓ JDBC
Supabase (PostgreSQL)
```

**Local development** uses `.env` / `frontend/.env.local` with `localhost`.  
**Production** uses host env vars only — no localhost in application source code.

---

## 1. Supabase (database)

1. Create a project on [Supabase](https://supabase.com).
2. **Settings → Database** → copy connection details.
3. Build a **JDBC URL** for Spring (required `jdbc:` prefix):

```text
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
```

4. Set on **Render** (not in git):

| Variable | Example |
|----------|---------|
| `DATABASE_URL` | `jdbc:postgresql://db.xxx.supabase.co:5432/postgres?sslmode=require` |
| `DATABASE_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` | *(from Supabase)* |

5. **Seed data:** export local DB and restore to Supabase, or re-run ingest scripts against prod API after deploy.

```bash
pg_dump -h localhost -U kleos -d kleos_transfers --no-owner --no-acl -Fc -f kleos.dump
# restore to Supabase via dashboard or pg_restore with Supabase host
```

Flyway runs automatically on backend startup (`ddl-auto: validate`).

---

## 2. Render (backend API)

### Option A — Blueprint

1. Push this repo to GitHub.
2. Render → **New → Blueprint** → connect repo (`render.yaml` at root).
3. Set secret env vars when prompted:
   - `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
   - `CORS_ALLOWED_ORIGINS` → your Vercel URL, e.g. `https://kleos-transfers.vercel.app`
4. Deploy. Note the service URL, e.g. `https://kleos-transfers-api.onrender.com`.

### Option B — Manual web service

- **Runtime:** Docker
- **Dockerfile path:** `backend/Dockerfile`
- **Docker context:** repository root
- **Health check path:** `/api/v1/health`
- **Environment:** `SPRING_PROFILES_ACTIVE=prod` plus DB + CORS vars above

### Verify

```bash
curl https://<your-service>.onrender.com/api/v1/health
curl https://<your-service>.onrender.com/actuator/health
```

Free tier may sleep after inactivity (cold start ~30–60s).

---

## 3. Vercel (frontend)

1. Import GitHub repo on [Vercel](https://vercel.com).
2. **Root Directory:** `frontend`
3. **Framework Preset:** Vite (auto)
4. **Environment variables** (Production):

| Variable | Value |
|----------|--------|
| `VITE_API_BASE_URL` | `https://<your-render-service>.onrender.com` |

No trailing slash. **Do not** use `localhost` in production env.

5. Deploy. URL: `https://<project>.vercel.app`

`frontend/vercel.json` handles SPA routing for React Router.

### Local frontend

```bash
cd frontend
cp .env.example .env.local
# VITE_API_BASE_URL=http://localhost:8080
npm run dev
```

---

## 4. CORS checklist

Render `CORS_ALLOWED_ORIGINS` must include **exact** browser origins:

```text
https://your-project.vercel.app
```

Add preview URLs if needed (comma-separated, no spaces).  
Local dev defaults remain `http://localhost:5173,http://127.0.0.1:5173` when unset.

After changing CORS, redeploy Render.

---

## 5. Environment variable reference

### Frontend (Vite)

| Variable | Local | Production |
|----------|-------|------------|
| `VITE_API_BASE_URL` | `http://localhost:8080` | `https://…onrender.com` |

This project uses **Vite**, not Next.js — use `VITE_API_BASE_URL` (not `NEXT_PUBLIC_*`).

### Backend (Spring Boot)

| Variable | Required | Description |
|----------|----------|-------------|
| `DATABASE_URL` | Yes | JDBC PostgreSQL URL |
| `DATABASE_USERNAME` | Yes | DB user |
| `DATABASE_PASSWORD` | Yes | DB password |
| `CORS_ALLOWED_ORIGINS` | Yes (prod) | Comma-separated UI origins |
| `SPRING_PROFILES_ACTIVE` | Recommended | `prod` on Render |
| `PORT` | Auto on Render | HTTP port (Render sets this) |

---

## 6. CI vs production

GitHub Actions builds the frontend with a placeholder API URL for typecheck only.  
Vercel production builds must set `VITE_API_BASE_URL` in the Vercel dashboard.

---

## Related issues

- [#75](https://github.com/Ysetia10/kleos-transfers/issues/75) Backend deploy
- [#76](https://github.com/Ysetia10/kleos-transfers/issues/76) Frontend deploy
- [#77](https://github.com/Ysetia10/kleos-transfers/issues/77) Go-live wiring
