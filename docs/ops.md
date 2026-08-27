# Production operations

Live stack (free tier):

| Layer | URL |
|-------|-----|
| Frontend | https://kleos-transfer.vercel.app |
| API | https://kleos-transfers-api.onrender.com |
| Database | Supabase project `kleos-transfers` (Session pooler JDBC on Render) |

See also [`deployment.md`](./deployment.md).

---

## Cold start (Render free tier) — #78

After ~15 minutes idle, Render spins the API down. The next request can take **~30–90s** after startup optimizations (previously **2–3 minutes**).

**Root causes addressed:**

1. **Keep-warm timed out too early** — `prod-ops` used a 90s curl limit while cold boot took ~179s, so scheduled pings failed before the API was UP.
2. **Slow Hibernate validate** — `ddl-auto: validate` against Supabase on every boot added ~90s on free-tier CPU.
3. **Infrequent pings** — 12-minute cron plus GitHub schedule drift let Render spin down between wakes.

**Mitigations in this repo:**

1. **Keep-warm** — `.github/workflows/keep-warm.yml` pings every **8 minutes**; **Production ops** pings every **10 minutes** as backup (hourly full smoke).
3. **Faster prod boot** — `application-prod.yml`: skip Flyway on boot (`SPRING_FLYWAY_ENABLED=false`), `ddl-auto: none`, lazy init, deferred JPA repos, springdoc off; Dockerfile JVM tier-1 compile.
4. **Frontend** — production timeout 180s, network retries, health prefetch on load, and a wake banner while waiting.
5. **Upgrade path** — Render paid / always-on removes spin-down entirely.

**When applying DB migrations in prod:** set `SPRING_FLYWAY_ENABLED=true` for one deploy (or run Flyway manually), then turn off again.

**Decision recorded:** keep-warm is ping-only with a long timeout; Flyway disabled on routine prod boots after schema is current.

---

## Monitoring — #82

| Check | How |
|-------|-----|
| API health | `GET /api/v1/health` must contain `"status":"UP"` |
| Data smoke | `GET /api/v1/clubs?size=1` must return `"content"` |
| Frontend | `GET https://kleos-transfer.vercel.app/` → HTTP 200 |

Workflow: **Production ops** (scheduled + `workflow_dispatch`). Failures notify via GitHub Actions (watch the repo / email notifications).

### Cold start vs hard down

| Symptom | Likely cause |
|---------|----------------|
| Health succeeds after 30–90s | Cold start (keep-warm missed or first wake) |
| Health fails for minutes | Crash loop — check Render logs (DB password, pooler URL, OOM) |
| Clubs empty / 500 | Data or Flyway issue — Supabase + Render logs |
| Browser CORS 403 | `CORS_ALLOWED_ORIGINS` missing exact Vercel origin |

**Render logs:** Dashboard → `kleos-transfers-api` → Logs.

---

## Rate limiting — #86

| Setting | Default |
|---------|---------|
| Enabled | `true` (`KLEOS_RATE_LIMIT_ENABLED`) |
| Limit | **60 requests / minute / IP** (`KLEOS_RATE_LIMIT_RPM`) |
| Excluded | `/api/v1/health`, `/actuator/**`, `OPTIONS` |
| Client IP | `X-Forwarded-For` (first hop), then `X-Real-IP`, then remote addr |
| Over limit | HTTP **429** + `Retry-After: 60` |

Tests disable the filter via `application-test.yml`.

---

## Backups — #85

Supabase free tier has limited automated backup retention. Do not rely on a single laptop dump.

### Monthly manual dump (owner)

```bash
# Prefer Session pooler host if your network is IPv4-only.
pg_dump -h aws-0-us-west-2.pooler.supabase.com \
  -U postgres.qrlqfeiokdbdhmbkclqj \
  -d postgres --no-owner --no-acl -Fc \
  -f "kleos-$(date +%Y%m%d).dump"
```

Store the dump in encrypted personal storage (not git). Rotate when the DB password changes.

### Restore drill (quarterly — owner)

1. Create a scratch Supabase project **or** use a local Postgres.
2. `pg_restore --no-owner --no-acl --clean --if-exists -d … kleos-YYYYMMDD.dump`
3. Point a throwaway Render/local API at it and hit `/api/v1/health` + `/api/v1/clubs?size=1`.
4. Record date + duration in this doc or the GitHub issue.

See `scripts/complete-supabase.sh` for the original go-live restore path.

---

## Storage (Supabase free) — #79

| Signal | Note |
|--------|------|
| Go-live dump | ~78 MB compressed local `pg_dump` |
| Free tier | ~500 MB database storage |
| Action | **No trim required** at go-live; re-check quarterly |

When auditing later:

```sql
SELECT relname AS table,
       pg_size_pretty(pg_total_relation_size(c.oid)) AS total
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'r'
ORDER BY pg_total_relation_size(c.oid) DESC
LIMIT 20;
```

Only trim prediction history / staging after a backup and product OK — never core squad identity.

---

## Localhost API URL guard — #84

- **Never** put `VITE_API_BASE_URL=http://localhost:8080` in `frontend/.env` (Vercel CLI can upload it).
- Local: `frontend/.env.local` from `.env.local.example`.
- Production: `frontend/vercel.json` → `build.env.VITE_API_BASE_URL`.
- CI: production-like Vite build must not contain `localhost:8080` in `dist/assets`.
