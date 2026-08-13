# Deployment Guide

## 1. Prerequisites

- Docker + Docker Compose v2 on the target host
- A MySQL-compatible managed database is recommended for production over
  the bundled `mysql` container (see §5) — the compose file's MySQL
  service is intended for local dev / small self-hosted deployments
- Outbound SMTP credentials (for invitation/notification email)
- A domain + TLS certificate if exposing this publicly (see §4)

## 2. Environment configuration

Copy `backend/.env.example` to `backend/.env` and fill in every value —
**there are no hardcoded defaults for secrets** by design (see
`application.yml`). At minimum:

| Variable | Notes |
|---|---|
| `JWT_SECRET` | Generate with `openssl rand -base64 64`. Rotating this invalidates every issued access token immediately and every refresh token on next use — rotate deliberately, not accidentally. |
| `DB_PASSWORD`, `DB_ROOT_PASSWORD` | Use a secrets manager in production (see §6), not a plaintext `.env` file on disk. |
| `CORS_ALLOWED_ORIGINS` | Must exactly match your frontend's public origin(s) — a mismatch here is the most common "why can't the frontend log in" support issue. |
| `MAIL_*` | Required once Sprint 8 (notifications/invitations) ships email sending. |
| `DEMO_DATA_ENABLED` | **Keep `false` in production.** When `true`, the backend seeds demo accounts (`pm@test.com` / `member@test.com`, password `DemoPass1`) plus a sample workspace/project/board on startup — intended for local evaluation only. The `.env.example` default of `true` is deliberate so a fresh clone is instantly explorable; flip it off on any shared or internet-facing host. |

The frontend has no server-side secrets — `frontend/.env.example` only
configures build-time API base URLs, which are safe to bake into the
static build.

## 3. First deploy

```bash
cp backend/.env.example backend/.env
# edit backend/.env with real values

docker compose up --build -d
docker compose ps        # wait for all services to report healthy
docker compose logs -f backend   # confirm Flyway migrations ran clean
```

Flyway runs automatically on backend startup (`spring.flyway.enabled: true`
in `application.yml`) — there is no separate manual migration step. The
backend's healthcheck won't report healthy until migrations succeed and
the Spring context fully starts, so `depends_on: condition: service_healthy`
on the frontend service means the frontend container won't accept traffic
against a backend that isn't actually ready.

## 4. Putting this behind TLS

The bundled `frontend` nginx container serves plain HTTP on port 80. For
any public deployment, put a TLS-terminating reverse proxy or load
balancer in front of it (a managed load balancer, or e.g. Caddy/Traefik
as an additional compose service) rather than modifying `nginx.conf` to
handle certificates directly — keeping TLS termination and the app
container decoupled makes certificate renewal an infra concern, not an
app-container concern.

Once behind TLS, also flip these to their production-safe values:
- `AuthController`'s refresh cookie is already `Secure` — this **requires
  HTTPS to work at all**; the cookie will silently not be set over plain
  HTTP, breaking login. This is a deliberate fail-closed choice, not a bug — don't weaken it to test over HTTP.
- Confirm `CORS_ALLOWED_ORIGINS` uses `https://`.

## 5. Database considerations for production

The compose file's `mysql` service uses a named volume (`mysql_data`) for
persistence, which survives `docker compose down` but **not**
`docker compose down -v`. For anything beyond small self-hosted use:

- Point `DB_HOST`/`DB_PORT` at a managed MySQL instance (RDS, Cloud SQL,
  PlanetScale, etc.) instead of the bundled container, and remove the
  `mysql` service from your deployment compose file entirely.
- Set up automated backups on whatever MySQL you land on — this repo
  does not implement application-level backup tooling.
- The soft-delete purge job (Sprint 10, not yet implemented) will
  eventually run destructive hard-deletes on a schedule — make sure
  backup retention accounts for that once it ships.

## 6. Secrets management

`backend/.env` is fine for local development but is a plaintext file on
disk — **do not deploy it as-is to a shared or internet-facing host**.
For real deployments, inject the same environment variables via your
platform's secrets mechanism instead (Docker Swarm secrets, Kubernetes
Secrets, your cloud provider's secrets manager, or your CI/CD platform's
encrypted environment variables) and never commit `backend/.env` — the
`.gitignore` already excludes it, keep it that way.

## 7. CI/CD

- `.github/workflows/ci.yml` — runs on every push/PR to `main`: backend
  tests (unit + controller + Testcontainers integration, Docker is
  preinstalled on GitHub-hosted runners) and frontend (lint + test +
  build), in parallel.
- `.github/workflows/docker-publish.yml` — builds and pushes both
  images to GitHub Container Registry (`ghcr.io`) on merge to `main`, or
  manually via `workflow_dispatch`. Tags each image with both `latest`
  and the commit SHA, so a specific deployed version is always
  traceable back to an exact commit.
- **Recommended branch protection**: require `ci.yml` to pass before
  merging to `main`, so `docker-publish.yml` only ever builds from a
  commit that already passed tests.

### Rollback

Since every published image is tagged with its commit SHA, rolling back
is: redeploy the previous known-good SHA's image tag rather than
`latest`. This repo doesn't include an orchestration-specific rollback
script (that depends on whether you land on Swarm, Kubernetes, or a
managed container platform) — but the SHA-tagged images are what make
any of those rollback mechanisms possible in the first place.

## 8. Post-deploy smoke test

```bash
curl https://your-domain/actuator/health          # via backend directly, or
curl https://your-domain/healthz                   # via the frontend nginx container
curl -X POST https://your-domain/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke-test@example.com","password":"SmokeTest1","fullName":"Smoke Test"}'
```

A successful register response confirms: the backend booted, Flyway
migrations succeeded, the database connection works, and CORS/cookie
settings are consistent with the domain you're testing from.
