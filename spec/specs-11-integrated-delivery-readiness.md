# Integrated UI Delivery Readiness Spec

**Status:** Approved  
**Depends on:** All earlier backend and frontend specs, [HLD](../docs/high-level-design.md), and [LLD](../docs/low-level-design.md)

## Why and what

Package the React UI and Spring Boot API as the promised single-origin application, document repeatable setup, and verify the complete assessment journey with PostgreSQL and the deterministic 10,000-employee seed.

## Build and runtime integration

- Build the frontend from its committed npm lockfile and produce static assets with hashed filenames. Include those assets in the runnable Spring Boot JAR and container image.
- Keep `/api/v1/**` and `/actuator/**` as backend routes. Serve `/`, the explicit SPA routes, and required static assets so the React application can render before authentication.
- Add an SPA fallback for valid frontend routes and direct refreshes. Never forward missing API or asset requests to `index.html`.
- Update Spring Security to permit the SPA entry point and public static assets while retaining session requirements for protected APIs. Client-side protected routing controls presentation only; backend authorization remains authoritative.
- Configure Vite development proxying for `/api` to local Spring Boot. Production uses relative same-origin URLs, session cookies, and CSRF headers without CORS configuration.
- Preserve the existing non-root runtime user, database environment configuration, readiness probes, graceful shutdown, and `SESSION_COOKIE_SECURE=true` requirement behind HTTPS.

## Documentation and review evidence

- Document frontend installation/development, backend startup, combined production build, Docker Compose startup, migrations, HR bootstrap, demo seed, sign-in, readiness checks, and focused verification commands.
- Document required environment variables, HTTPS termination, persistent PostgreSQL, and hosting-provider backup responsibility without committing secrets.
- With the 10,000-employee seed, record environment and observed timings for an early employee page, a later page, the country report, and the country/job-title report. Do not impose a machine-specific latency threshold; investigate visibly slow indexed paths.
- Prepare a demo checklist covering sign-in, employee search/detail, employee create or edit, salary change, both required report questions, and a small CSV preview/commit.

## Quality gates

- Frontend lint, type check, unit/component tests, and production build pass. Backend tests and Maven package pass. The packaged container starts against PostgreSQL and becomes ready.
- Playwright covers sign-in, employee search/detail, one employee mutation, and both required reports. Add one focused CSV preview/commit scenario because import is supporting scope.
- Run automated accessibility checks on sign-in, employee directory, employee form, and reports; manually verify keyboard focus, dialogs, navigation, and responsive behavior at 360 px, tablet, and desktop widths.
- Confirm health endpoints remain independent of SPA routing. Confirm anonymous users can load the sign-in shell but protected APIs return `401`.
- Confirm browser and application logs exclude credentials, salary amounts, raw CSV contents, session cookies, and CSRF tokens.

## Decisions and non-goals

Chromium is the automated browser target; current Chrome, Firefox, and Safari receive focused manual smoke checks. The application stays a single deployable container plus persistent PostgreSQL. Server-side rendering, a service worker, a CDN requirement, client telemetry, and a separately deployed frontend are outside scope. Public deployment and video recording follow successful local integrated verification.

## Acceptance scenarios

- A clean checkout builds and runs the complete application using the documented commands.
- Direct navigation and refresh work for every SPA route; missing APIs and assets retain correct HTTP errors.
- Static assets and sign-in load anonymously, while every protected API continues to require the HR session and CSRF on mutations.
- UI and API work from one origin, and the core employee and reporting journeys remain usable with 10,000 seeded employees.
- Docker readiness, sign-in, state-changing requests, and sign-out work in the packaged image.
- The repository contains no committed credentials or supplied assessment document, and the incremental specification/implementation history remains reviewable.
