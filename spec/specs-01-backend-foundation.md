# Backend Foundation Spec

**Status:** Draft for review  
**Depends on:** [Requirements](../docs/requirements.md) and [LLD](../docs/low-level-design.md)

## Why and what

Establish a runnable Spring Boot API with one set of persistence, validation, error, and test conventions. This foundation lets feature modules be implemented independently within the agreed modular monolith.

## Scope and contracts

- Use Java, Spring Boot, Maven, and PostgreSQL. Keep HTTP controllers, domain services, and repositories in separate `auth`, `employees`, `salaries`, `imports`, and `reports` modules. Service methods own transaction boundaries.
- Manage the schema with versioned migrations. Read database credentials and runtime settings from environment variables. Fail startup when required production settings are missing.
- Prefix application endpoints with `/api/v1`. Serialize dates as `YYYY-MM-DD`, money as decimal strings, and IDs as UUIDs. Use an injectable UTC clock for date-dependent rules and deterministic tests.
- Return validation and domain failures as `application/problem+json` with `type`, `title`, `status`, `detail`, and optional field or row `errors`. Use the LLD status codes consistently. Never expose stack traces or SQL details in API responses.
- Use one-based pagination (`page=1`, default `size=25`, maximum `size=100`) and stable tie-breaking for every list endpoint. Add request IDs and structured logs while excluding credentials, salary values, and imported file contents.

## Decisions and non-goals

A single application and database keep transactions and deployment simple at this scale. Feature migrations create their own tables; this spec establishes the migration mechanism and shared conventions. Queues, microservices, public API consumers, and generic framework abstractions are outside scope.

## Acceptance scenarios

- A clean PostgreSQL database accepts the migrations and the API starts; a migration failure prevents readiness.
- Invalid input produces a stable problem response without sensitive internals. Pagination rejects invalid values and caps oversized requests.
- Unit tests use the injected clock; focused database tests run against PostgreSQL rather than relying on database-specific behavior in an in-memory substitute.
