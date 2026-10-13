# Salary Management

Backend foundation for the salary management assessment. The application uses Java 21, Spring Boot, Maven, and PostgreSQL.

## Local setup

1. Start a PostgreSQL database.
2. Set `DATABASE_URL` (for example, `jdbc:postgresql://localhost:5432/salary_management`), `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in your shell.
3. Run `mvn spring-boot:run` (or `mvn test` to run the unit tests).

The service listens on port 8080. Health and readiness probes are available under `/actuator/health`. Flyway runs migrations at startup; Hibernate validates the schema. No database credentials are committed to the repository.

## Bootstrap the HR account

Set `HR_INITIAL_EMAIL` and `HR_INITIAL_PASSWORD` from deployment secrets, then run the application once with `--spring.main.web-application-type=none --app.bootstrap.hr-user.enabled=true` to create the account. Bootstrap is disabled by default. Running it again for the same normalized email keeps the existing password; a different email fails rather than creating a second account. Clear the bootstrap flag after the account has been created.

The session API is `GET`, `POST`, and `DELETE /api/v1/session`. The UI should first call `GET` to receive the CSRF cookie, send its value as `X-XSRF-TOKEN` on sign-in and other mutations, then call `GET` again after sign-in to obtain a fresh token. Authenticated API responses use `Cache-Control: no-store`. Set `SESSION_COOKIE_SECURE=true` in deployed HTTPS environments; local HTTP development defaults to `false`.

Sign-in is limited to 10 attempts per five minutes per client IP. If the app is behind a trusted ingress, set `TRUSTED_PROXY_ADDRESSES` to its comma-separated socket IP addresses; the ingress must replace `X-Forwarded-For` with the original client address. Forwarded headers from any other source are ignored.

## Backend module layout

Feature code belongs in `auth`, `employees`, `salaries`, `imports`, and `reports` packages under `com.incubyte.salarymanagement`. Shared HTTP contracts live under `api`; cross-cutting configuration and observability have their own packages.
