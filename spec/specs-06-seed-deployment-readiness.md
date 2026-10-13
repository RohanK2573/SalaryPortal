# Seed and Deployment Readiness Spec

**Status:** Approved
**Depends on:** All earlier backend specs and [LLD](../docs/low-level-design.md)

## Why and what

Make the backend repeatable to set up, usable with the assessment's 10,000 employees, and ready for a deployed demo. This stage delivers backend packaging and evidence; the deployed UI and API are verified together after the frontend is built.

## Demo seed contract

- Provide an explicit, opt-in command, such as `java -jar salary-management.jar --spring.main.web-application-type=none --app.demo-seed.enabled=true`. Ordinary application startup never seeds demo employees.
- On a clean database, create exactly 10,000 `ACTIVE` synthetic employees, each with one current gross annual base salary. Use stable employee codes, a fixed generation seed, and a fixed past effective date so the initial dataset is reproducible. Include multiple countries, local currencies, departments, and job titles so both required report questions have meaningful results.
- Treat the 10,000 count as **seed-owned employees**. A database with manually entered employees may have more than 10,000 employees in total. Preserve all non-seed employees and salary records.
- Rerunning the seed must not duplicate or overwrite employees or salaries. Preserve manual data; if a seed code is already used by a different employee, stop with a clear error rather than replace that record.

## Backend runtime and delivery

- Package the Spring Boot backend as a runnable container image that connects to persistent PostgreSQL through environment configuration. Flyway applies versioned migrations before the API becomes ready; migration or database connection failure prevents startup/readiness.
- Expose `/actuator/health/readiness` with the database health check included. Return a healthy response while PostgreSQL is available and an unhealthy response when it is unavailable. Keep response details hidden; liveness should remain independent of a transient database failure.
- Document container build and run commands, required environment variables, local PostgreSQL setup, migrations, HR-account bootstrap, the demo seed command, API startup, and focused backend test commands. Use deployment secrets for credentials and never place them in source files or application logs.
- Document HTTPS termination, `SESSION_COOKIE_SECURE=true`, and persistent PostgreSQL for the deployed demo; note how the chosen host handles backups. After the frontend stage, include the React build in the same-origin application image and verify the deployed UI and API together.
- Verify the existing HR bootstrap, Flyway startup, request ID logging, and safe request outcomes; do not rebuild these capabilities in this stage. Health responses and application logs must not include credentials, salary amounts, or CSV contents.

## Repeatable scale check

With the clean 10,000-employee seed, check an early and a later employee page plus the country and country/job-title compensation reports. Record the request timings and environment in a short review note; inspect PostgreSQL query plans if a path is unexpectedly slow. There is no machine-dependent fixed latency threshold.

## Decisions and non-goals

The seed never overwrites existing HR-managed data. One application image plus persistent PostgreSQL matches the HLD; a specific cloud vendor is not part of the backend contract. Multi-instance sessions, production payroll operations, and importing real employee data are outside this assessment release.

## Acceptance scenarios

- A clean database migrates and seeds to exactly 10,000 employees and 10,000 current salary records. Repeating the seed adds no duplicates or changes. Manually entered employees survive, and a conflicting employee code is reported without overwriting data.
- The backend image starts with valid settings. `/actuator/health/readiness` is healthy with PostgreSQL available and unhealthy when it is unavailable. Health responses and application logs contain no credentials or salary values.
- Setup instructions work from a clean checkout. The documented scale check covers employee pages and the two required reports with the full seed and records timings and any performance issue found.
