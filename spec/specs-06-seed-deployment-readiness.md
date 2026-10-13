# Seed and Deployment Readiness Spec

**Status:** Draft for review  
**Depends on:** All earlier backend specs and [LLD](../docs/low-level-design.md)

## Why and what

Make the backend repeatable to set up, realistic at the required size, and ready to support a deployed end-to-end assessment demo.

## Scope and behavior

- Provide an explicit demo seed command that creates exactly 10,000 synthetic employees with current salary records across multiple countries, departments, job titles, and currencies. A fixed seed and stable employee codes make repeated runs produce the same dataset without duplicates. Do not run demo seeding automatically in a non-demo environment.
- Provide the idempotent HR-account bootstrap described in the authentication spec. Credentials and database settings come from deployment secrets, not source files or log output.
- Build a deployable Spring Boot container configured for persistent PostgreSQL. Apply versioned database migrations at startup and fail readiness when migrations or database access fail. Expose a health/readiness endpoint without sensitive data.
- Document local setup, environment variables, migration and seed commands, API startup, and how to run the focused backend tests. The final single-origin container will include the built React assets after the frontend stage; the backend remains runnable and deployable before those assets exist.
- Capture request IDs and safe operational outcomes in logs. Configure HTTPS termination, secure session cookies, and database backups for the deployed demo.

## Decisions and non-goals

An explicit seed protects real data from accidental replacement. One application container plus persistent PostgreSQL matches the HLD; a particular cloud vendor is not part of the application contract. Multi-instance sessions, production payroll operations, and importing real employee data are outside this assessment release.

## Acceptance scenarios

- A clean database migrates and seeds to exactly 10,000 employees; rerunning the seed leaves the same count and data.
- The API starts with valid configuration, reports unhealthy when PostgreSQL is unavailable, and never exposes secrets or salaries through health responses or logs.
- With the full seed, employee pages and compensation reports remain responsive; record query plans and timings for the main paths. Backend setup instructions work from a clean checkout.
