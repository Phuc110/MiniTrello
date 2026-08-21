# Mini Trello Enterprise

Enterprise-grade project management platform (Trello/Jira-style) — Java 21 / Spring Boot 3 backend, React 19 / TypeScript frontend.

## Quickstart

```bash
./start.sh
```

That's it — this generates a real `JWT_SECRET`, builds every image, and
brings up mysql + redis + backend + frontend via Docker Compose. Requires
Docker Desktop (or Docker Engine + Compose v2) to be installed and
running. First run takes a few minutes; subsequent runs are fast thanks
to Docker's build cache.

Once it's up:
- Frontend: http://localhost
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

Stop everything with `docker compose down` (add `-v` to also wipe the database volume).

## Demo accounts

When `backend/.env` has `DEMO_DATA_ENABLED=true` (it is set by default in
`.env.example`), the backend seeds a small demo dataset on first startup:
two accounts, a shared workspace, one project with a populated Kanban
board (tags, assignees, priorities, due dates). Login on the demo seed:

| Role | Email | Password |
|---|---|---|
| Project Manager | `pm@test.com` | `DemoPass1` |
| Member | `member@test.com` | `DemoPass1` |

Both land in the same "Demo Workspace" → "Website Launch" project, so you
can open two browsers and watch drag-and-drop changes in real time. The
seed is idempotent (skips if already present) and runs **only** when
`DEMO_DATA_ENABLED=true` — keep it `false` in production.

This repository is being built phase-by-phase per the project's system prompt workflow. **Current status: Phase 8 — Frontend scaffold complete.**

## Repository layout

```
backend/     Spring Boot 3 API — Clean Architecture (domain/application/infrastructure/presentation)
frontend/    React 19 + Vite + TypeScript SPA
docker-compose.yml   Orchestrates backend + mysql + redis + frontend
```

## What's implemented so far

**Backend** (Phases 4–7): Backend Foundation, Authentication (JWT + rotating
refresh tokens), Project Module (Workspaces, Projects, role-based
membership), Task Module (Boards, Lists, Tasks, drag-and-drop ordering,
priority/tags/deadlines). See `backend/README.md`-equivalent context in
the conversation history for full per-phase rationale — full detail also
lives in code comments throughout `backend/src`.

**Frontend** (Phase 8): Vite + React 19 + TypeScript + Tailwind. Axios
client with automatic single-flight access-token refresh. TanStack Query
for server state. React Hook Form + Zod for validated forms. Auth flow
(login/register/silent-refresh-on-reload). Workspace and Project list
pages with search/pagination/loading/error/empty states. A fully
drag-and-drop Kanban board (`@hello-pangea/dnd`) with task creation,
editing, and cross-list movement, wired to the backend's neighbor-based
positioning API. See `frontend/README-design-notes.md` for the visual
design rationale.

## Running locally

```bash
docker compose up --build          # zero-config: dev defaults apply
# recommended — customize secrets / enable demo data:
cp backend/.env.example backend/.env   # at minimum set your own JWT_SECRET
```

The stack boots out of the box with placeholder dev credentials
(`change_me` DB password, `dev_only_…` JWT secret). `backend/.env` is
optional but overrides everything when present; never reuse the dev
defaults outside local development.

Backend: `http://localhost:8080` · Swagger UI: `http://localhost:8080/swagger-ui.html`
Frontend (dev server): `http://localhost:5173` — proxies `/api` and `/ws` to the backend (see `frontend/vite.config.ts`)
Frontend (docker/prod build): `http://localhost:80` — nginx serves the static build and reverse-proxies `/api`/`/ws`

For frontend-only local development without Docker:
```bash
cd frontend
npm install
npm run dev
```

## What's NOT yet built

Per the roadmap's Sprints 6–9 (Comments/Attachments/Activity Timeline,
Dashboard/Charts, Notifications/WebSocket real-time, Audit Logs) — these
were intentionally deferred, since the system prompt's 10-phase workflow
jumps from "Task Module" straight to "Frontend" to "Testing" to
"Deployment." Flagged explicitly at the end of Phase 7 rather than
silently skipped.

## Testing

**Backend** — four distinct test categories, per the project's testing requirements:
- `src/test/java/com/minitrello/unit/` — Mockito, zero Spring context, zero database. Fast, exercises business rules and permission matrices in isolation.
- `src/test/java/com/minitrello/controller/` — standalone `MockMvc` wired directly to a controller with a mocked service layer (no Spring context, no security filter chain). Verifies request validation, status codes, and the `ApiResponse` envelope.
- `src/test/java/com/minitrello/integration/` — full HTTP-to-database flows via Testcontainers MySQL, including a dedicated repository-layer test for the security-critical membership-scoped project query.
- Run with `./mvnw test` (Testcontainers tests need Docker available).

**Frontend** — Vitest + React Testing Library:
- `npm test` — runs once (CI mode); `npm run test:watch` for local development.
- Component tests wrap `@hello-pangea/dnd` components in a minimal `DragDropContext`/`Droppable` harness, since those components throw outside that context.
- Zod schema tests are kept in sync with the backend's Bean Validation rules (e.g. password complexity) — a mismatch between the two would mean confusing server-side 400s after client-side validation already passed.

## Status

**Current status: Phase 10 — Deployment. All 10 phases of the original workflow are now scaffolded.**

See `DEPLOYMENT.md` for the full production deployment guide, and
`.github/workflows/` for CI (`ci.yml`) and image publishing
(`docker-publish.yml`).

**What was deliberately deferred throughout** (flagged at the point each
decision was made, not silently dropped): Sprints 6–9 from the original
roadmap — Comments/Attachments/Activity Timeline, Dashboard/Charts,
real-time WebSocket notifications, and Audit Logs. The 10-phase workflow
in the project brief jumps from "Task Module" straight to "Frontend,"
"Testing," "Deployment" — I followed that structure literally rather than
assuming it should also cover the sprints in between. If you want any of
those built out next, just say which one and I'll treat it as its own
scoped addition, with the same design-first approach used throughout.



This project follows a strict phase-gated build process — see the project's system prompt.
Nothing beyond the current phase is generated ahead of schedule.

