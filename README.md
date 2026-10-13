# Salary Management

A salary management assessment application. The React UI and Spring Boot API are packaged together and share one origin. PostgreSQL stores employee data, salary history, import previews, and audit events.

## Start a local demo

Requirements: Docker Desktop installed and running.

```sh
cp .env.example .env
docker compose build app
docker compose up -d postgres
docker compose run --rm app --spring.main.web-application-type=none --app.bootstrap.hr-user.enabled=true
# Optional: load the 10,000 demo employees.
docker compose run --rm app --spring.main.web-application-type=none --app.demo-seed.enabled=true
docker compose up -d app
```

Set `HR_INITIAL_EMAIL` and `HR_INITIAL_PASSWORD` in `.env` before running bootstrap. Open `http://localhost:8080/signin` and sign in with those values. The example values are for local development only.

To include the 10,000 synthetic demo employees and salary records, first start PostgreSQL and then run the seed command below. This takes longer; without seeding, the app starts with an empty database and you can add employees through the UI.

Compose builds the app and UI, starts PostgreSQL, and waits for database readiness. PostgreSQL runs on port 5433 and its data persists in the `salary_management_postgres_data` volume. Stop the services with `docker compose down`; remove the local database too with `docker compose down -v`.

## Development

Requirements: Java 21, Maven, Node.js 22 or newer, and npm.

For the Vite development server, start PostgreSQL and the backend, then run:

```sh
cd frontend
npm ci
npm run dev
```

Vite serves the UI and proxies `/api` and `/actuator` to `http://localhost:8080`.

To build the integrated JAR from a clean checkout:

```sh
cd frontend
npm ci
npm run typecheck
npm run build
cd ..
mvn clean test package
```

The Maven build includes `frontend/dist` in the JAR under `static/`. The Docker build runs the same frontend and backend build steps.

## Runtime and deployment

The container runs as a non-root user and listens on port 8080. Configure these values in the hosting environment or secret manager; do not commit production secrets.

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Application database user |
| `DATABASE_PASSWORD` | Application database password |
| `SESSION_COOKIE_SECURE` | Set to `true` when HTTPS terminates at the hosting ingress |
| `HR_INITIAL_EMAIL` | Initial HR account email, used during bootstrap |
| `HR_INITIAL_PASSWORD` | Initial HR account password, used during bootstrap |
| `TRUSTED_PROXY_ADDRESSES` | Trusted ingress socket IPs for forwarded client IP handling |

Deploy one application container with a persistent PostgreSQL service. Configure HTTPS at the hosting ingress and set `SESSION_COOKIE_SECURE=true`. The hosting provider or platform owner must configure database backups, retention, encryption, and a tested restore procedure; document the selected provider's instructions in its deployment runbook.

Flyway applies database migrations during startup and Hibernate validates the schema. Readiness at `/actuator/health/readiness` includes PostgreSQL; liveness is `/actuator/health/liveness`. Health details are hidden.

The browser and API share one origin. Session cookies are HTTP-only and use `SameSite=Lax`; the browser returns the readable CSRF cookie in the `X-XSRF-TOKEN` header for mutations. CORS is not needed.

## 10,000-employee scale check

With the 10,000 employee seed loaded and an authenticated HR session, measure these API requests and record machine/container CPU and memory, PostgreSQL version, application build, and observed times. No machine-specific latency threshold is required; inspect query plans if a path looks slow.

- Employee page 1: `/api/v1/employees?page=1&size=25&sort=employeeCode,asc`
- Employee page 400: `/api/v1/employees?page=400&size=25&sort=employeeCode,asc`
- Country report: `/api/v1/reports/compensation?groupBy=country`
- Country/job-title report: `/api/v1/reports/compensation?groupBy=countryJobTitle&countryCode=IN`

Record measured results in the delivery notes for the environment used. This repository does not include fabricated timings.

## Demo checklist

- Sign in and sign out; anonymous employee API calls return `401`.
- Search an employee, open details, create an employee, and edit a profile.
- Add a dated salary and confirm current salary and history refresh.
- Review country salary minimum, maximum, and average, then average salary by job title within a country. Confirm currencies remain separate.
- Download the CSV template, preview a small file, inspect its rows, and commit it.
- Refresh `/employees`, `/employees/new`, `/employees/{id}`, `/reports`, `/imports`, and `/imports/{id}` directly.
- Check keyboard focus, dialogs, navigation, and responsive layouts at mobile, tablet, and desktop widths.

## Repository contents

- `src/`: Spring Boot API, migrations, authentication, seed, and health configuration.
- `frontend/`: React/TypeScript UI and committed npm lockfile.
- `spec/`: ordered implementation specifications and changelog.
- `docs/`: requirements and system designs.
