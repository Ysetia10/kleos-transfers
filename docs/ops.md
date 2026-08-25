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

After ~15 minutes idle, Render spins the API down. The next request can take **~30–60s**.

**Mitigations in this repo:**

1. **Keep-warm + uptime** — `.github/workflows/prod-ops.yml` curls `/api/v1/health` every ~12 minutes (under the idle window).
2. **Frontend** — production timeout 60s, network retries, health prefetch on load, and an “Starting the API…” banner while waiting.
3. **Upgrade path** — Render paid / always-on removes spin-down; keep docs updated if you upgrade.

**Decision recorded:** use GitHub Actions keep-warm on free tier; do not disable Flyway on boot.

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
