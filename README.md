# Salary Management

Salary management assessment application built with Java 21, Spring Boot, Maven, and PostgreSQL.

## Local setup with Docker Compose

1. Start the app and PostgreSQL: `docker compose up --build -d app`.
2. Check startup and readiness: `docker compose ps` and `curl -i http://localhost:8080/actuator/health/readiness`.
3. Run the backend tests with `mvn test` (Java 21 and Maven required).

The app listens on port 8080. Compose uses local development credentials only; do not reuse them outside a local machine. PostgreSQL data persists in the `salary_management_postgres_data` volume. `docker compose down` stops the containers; `docker compose down -v` also deletes that local database volume.

## Frontend development

The React and TypeScript UI lives in `frontend/`. With the backend running on port 8080, start the development server with `cd frontend && npm install && npm run dev`; Vite serves the UI and proxies `/api` requests to Spring Boot. Build the production UI with `cd frontend && npm run build`. The integrated container build is covered by the final UI delivery spec.

## Frontend development

The React and TypeScript UI lives in `frontend/`. With the backend running on port 8080, start the development server with `cd frontend && npm install && npm run dev`; Vite serves the UI and proxies `/api` requests to Spring Boot. Build the production UI with `cd frontend && npm run build`. The integrated container build is covered by the final UI delivery spec.

For IntelliJ or Maven startup outside Docker Compose, start only PostgreSQL with `docker compose up -d postgres`, then set `DATABASE_URL=jdbc:postgresql://localhost:5433/salary_management`, `DATABASE_USERNAME=salary_app`, and `DATABASE_PASSWORD=local_dev_password` in the run configuration. Flyway applies migrations at startup and Hibernate validates the schema.

## Demo seed

Seeding is opt-in. After starting Compose, run:

```sh
docker compose run --rm app --spring.main.web-application-type=none --app.demo-seed.enabled=true
```

On a clean database this creates 10,000 deterministic synthetic employees and current salary records. Reruns do not duplicate or overwrite records. Existing non-demo employees are preserved; a conflicting `DEMO-` employee code makes the command fail without replacing that employee.

## Bootstrap the HR account

Set `HR_INITIAL_EMAIL` and `HR_INITIAL_PASSWORD` from local environment variables or deployment secrets, then run:

```sh
docker compose run --rm app --spring.main.web-application-type=none --app.bootstrap.hr-user.enabled=true
```

Bootstrap is disabled by default. Repeating it with the same normalized email keeps the existing password; a different email fails rather than creating another account. Do not store the credentials in source files or pass them as command-line arguments.

The session API is `GET`, `POST`, and `DELETE /api/v1/session`. The UI first calls `GET` to receive the CSRF cookie, sends its value as `X-XSRF-TOKEN` on sign-in and other mutations, then calls `GET` again after sign-in to obtain a fresh token. Authenticated API responses use `Cache-Control: no-store`. Set `SESSION_COOKIE_SECURE=true` in deployed HTTPS environments; local HTTP development defaults to `false`.

Sign-in is limited to 10 attempts per five minutes per client IP. If the app is behind a trusted ingress, set `TRUSTED_PROXY_ADDRESSES` to its comma-separated socket IP addresses; the ingress must replace `X-Forwarded-For` with the original client address. Forwarded headers from any other source are ignored.

## Backend image and deployment

Build the backend image with `docker build -t salary-management:local .`. Configure `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in the deployment environment. Keep PostgreSQL persistent and set `SESSION_COOKIE_SECURE=true` when HTTPS terminates at the hosting ingress. Configure database backups through the chosen host and document its restore procedure. The final single-origin image will also include the React build after the frontend stage.

`/actuator/health/liveness` checks whether the process is alive. `/actuator/health/readiness` also checks PostgreSQL and returns unhealthy when the database is unavailable. Health details are hidden. Flyway migration failures prevent the service from starting.

## Backend module layout

Feature code belongs in `auth`, `employees`, `salaries`, `imports`, and `reports` packages under `com.incubyte.salarymanagement`. Shared HTTP contracts live under `api`; cross-cutting configuration and observability have their own packages.
