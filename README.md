# Salary Management

Backend foundation for the salary management assessment. The application uses Java 21, Spring Boot, Maven, and PostgreSQL.

## Local setup

1. Start a PostgreSQL database.
2. Set `DATABASE_URL` (for example, `jdbc:postgresql://localhost:5432/salary_management`), `DATABASE_USERNAME`, and `DATABASE_PASSWORD` in your shell.
3. Run `mvn spring-boot:run` (or `mvn test` to run the unit tests).

The service listens on port 8080. Health and readiness probes are available under `/actuator/health`. Flyway runs migrations at startup; Hibernate validates the schema. No database credentials are committed to the repository.

## Backend module layout

Feature code belongs in `auth`, `employees`, `salaries`, `imports`, and `reports` packages under `com.incubyte.salarymanagement`. Shared HTTP contracts live under `api`; cross-cutting configuration and observability have their own packages.
