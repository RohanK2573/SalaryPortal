# Change Log

## 2026-10-13

- Added `docs/low-level-design.md` to define the agreed architecture, data model, API boundaries, workflows, security, and verification approach for the salary management assessment.
- Added `docs/high-level-design.md` with a system diagram, primary workflows, deployment view, and key architecture decisions.
- Added six draft backend feature specs covering foundation, HR access, employee and salary management, CSV import, compensation reporting, and seed/deployment readiness. Clarified public session bootstrap and CSRF handling in the LLD.
- Numbered backend spec filenames by implementation order and added this index for dependency navigation.
- Added a project `.gitignore` for build output, local environment secrets, and machine-specific files.
- Marked the backend foundation spec approved and started its implementation.
- Implemented the Spring Boot/Maven application skeleton, required PostgreSQL configuration, Flyway migration baseline, shared problem responses, one-based pagination, UTC clock, request IDs, and focused unit tests for the shared conventions.
- Refined the draft HR authentication spec with the single-account bootstrap behavior, 30-minute idle timeout, CSRF refresh and sign-out rules, non-cacheable responses, and sign-in rate limiting.
- Marked the HR authentication spec approved and started its implementation.
- Added the HR account schema and bootstrap, password authentication, server-side session endpoints, CSRF protection, protected API routing, client IP rate limiting, and authentication unit tests.
- Expanded the draft employee and salary management spec with step-by-step algorithms for its seven API operations, shared validation and error rules, transaction boundaries, audit behavior, and review decisions.
- Marked the employee and salary management spec approved.
- Implemented employee search and pagination, employee profile APIs, dated salary history and corrections, PostgreSQL constraints and indexes, and transactional salary audit events.
- Added a Docker Compose PostgreSQL service and local setup instructions to support repeatable database-backed development and verification.
- Added the Spring Boot 4 Flyway starter after local startup verification showed that the Flyway engine dependency alone did not enable migration auto-configuration.
- Removed the obsolete Jackson 2 date serialization property; Jackson 3 uses `DateTimeFeature` and serializes Java date/time values as ISO-8601 by default.
- Updated CSRF handling for SPA requests after local sign-in returned `403`; Spring Security now accepts the readable cookie token in the configured header. Added the local IntelliJ properties file to `.gitignore`.
