# Salary Management Low-Level Design

**Status:** Approved design baseline for feature specs  
**Product scope:** [Salary Management Requirements](./requirements.md)  
**Primary user:** ACME HR manager

## 1. System architecture

The application is a modular monolith. A React and TypeScript frontend is built as static assets and served by the Spring Boot application. Browser requests to `/api/v1` reach the same origin, avoiding cross-origin session configuration. Spring Boot owns authentication, validation, business rules, and persistence in PostgreSQL.

```text
Browser (React) -> Spring Boot controllers -> domain services -> repositories -> PostgreSQL
                                         \-> server-side session and audit events
```

Backend modules are `auth`, `employees`, `salaries`, `imports`, and `reports`. Controllers handle HTTP concerns; services enforce rules and transactions; repositories own queries. The frontend has corresponding feature areas and one API client that handles session expiry and structured errors. A single service and database are sufficient for the 10,000-employee assessment; no queue or microservice is needed.

## 2. Data model and invariants

| Table | Main fields | Constraints and purpose |
| --- | --- | --- |
| `employee` | `id` UUID, `employee_code`, `full_name`, `country_code`, `department`, `job_title`, `status`, `version`, timestamps | Unique `employee_code`; `status` is `ACTIVE` or `INACTIVE`. `version` detects concurrent profile edits. |
| `salary_record` | `id` UUID, `employee_id`, `annual_base_amount` NUMERIC(19,4), `currency_code`, `effective_date`, `version`, timestamps | Positive amount; ISO currency code; unique (`employee_id`, `effective_date`); foreign key to employee. A version supports audited corrections. |
| `audit_event` | `id`, actor, employee, event type, source, before/after values, timestamp, optional import batch | Records salary creation and correction, including changes made by import. Audit records are not editable through the product. |
| `hr_user` | `id`, email, password hash, role, active flag | One seeded HR account; no public registration or additional role workflow. |
| `import_batch` and `import_row` | owner, status, creation/expiry times, file hash, row number, normalized row, proposed action, errors, expected employee and latest-salary versions, result | Hold a reviewed CSV preview and make commit repeatable. Preview rows expire after 24 hours. |

Employee codes are stable external identifiers. Country and currency use ISO codes, but the model does not assume every employee in a country is paid in the same currency. Salary changes may be effective in the past or today; future dates are rejected. The **current salary** is the record with the latest `effective_date` on or before the server's UTC date. A new dated record preserves history. Correcting a record on an existing date updates that record with optimistic locking and writes an audit event; deleting salary history is not supported.

Reports include `ACTIVE` employees with a current salary. They group amounts by currency and never sum different currencies. Add indexes on employee code, employee search/filter fields, and (`employee_id`, `effective_date` descending). Migrations own schema changes; the seed script uses a fixed random seed and stable employee codes so reruns do not duplicate records.

## 3. API contract and behavior

All endpoints use `/api/v1`, JSON except CSV upload, and require an HR session unless stated otherwise. IDs are UUIDs, dates are `YYYY-MM-DD`, and money amounts are decimal **strings** in API payloads to avoid JavaScript rounding. List endpoints use one-based `page` and `size` parameters, a maximum page size of 100, a stable employee-code tie-breaker, and a response containing `items`, `page`, `size`, and `total`.

| Endpoint | Behavior |
| --- | --- |
| `POST /session`, `GET /session`, `DELETE /session` | Sign in, read current session state, and sign out. Public `GET /session` also initializes the CSRF token needed for sign-in. |
| `GET /employees` | Search name or employee code; filter by country, department, and status; paginate and sort. |
| `POST /employees`, `GET /employees/{id}`, `PATCH /employees/{id}` | Create with initial salary in one transaction; read details; update profile using the supplied version. |
| `GET /employees/{id}/salary-records`, `POST /employees/{id}/salary-records` | Read dated history and append a past-or-today salary change. |
| `PATCH /employees/{id}/salary-records/{recordId}` | Correct an existing record with its version and a reason; write an audit event. |
| `POST /imports/preview`, `GET /imports/{id}`, `POST /imports/{id}/commit` | Upload and validate CSV, inspect the staged preview, and commit the entire batch atomically. |
| `GET /reports/compensation` | Return current headcount, total, average, and median annual base salary, grouped by country or department and currency, with country/department filters. |

Validation errors use a consistent problem response with field or CSV row errors. The API returns `401` for no session, `403` for denied access, `413` for an oversized upload, `422` for invalid input, `409` for a concurrent edit or stale import preview, and `410` for an expired preview. The UI preserves unsaved input and offers a clear retry or refresh action for recoverable failures.

### CSV import lifecycle

The CSV template contains `employee_code`, `full_name`, `country_code`, `department`, `job_title`, `status`, `annual_base_amount`, `currency_code`, and `effective_date`. Preview parses and validates every row, rejects duplicate employee codes **within the file**, and classifies rows as create, update, no-op, or invalid. An employee code already in the database is an update, not a duplicate. A changed salary on an existing employee must have an effective date later than that employee's latest salary record; same-date corrections use the salary correction flow.

The batch moves through `PREVIEWED -> COMMITTED` or `PREVIEWED -> EXPIRED`. Commit is allowed only when every row is valid. In one database transaction it locks the batch, confirms that proposed creates are still new and that existing employees and their latest salary records match previewed versions, applies profile changes and new salary records, writes audit events, and saves the result. If data changed after preview, commit returns `409` and asks HR to upload a fresh preview. Repeating a committed request returns the saved result without applying changes again. Expired previews are removed by a scheduled cleanup.

## 4. Product experience and operations

The frontend contains sign-in, overview, employee directory, employee detail and history, salary edit, import preview, and reporting views. Forms show currency and effective date explicitly. Imports show action counts and row errors before the commit button is enabled. Every view has loading, empty, and error states, keyboard-accessible controls, and responsive layouts. The browser requests paged data and server-generated aggregates rather than downloading all employees.

Spring Security manages the HR session with an `HttpOnly`, `SameSite=Lax` cookie that is also `Secure` over deployed HTTPS. Public `GET /session` initializes a readable CSRF token cookie; the UI sends that token in a request header for mutations, including sign-in. Passwords are hashed; credentials and database settings come from deployment secrets. Structured logs include request IDs and outcomes but exclude salary values, passwords, and CSV contents. A health endpoint checks application and database readiness. The deployable container includes the React build and Spring Boot API; PostgreSQL is persistent and backed up. Database migrations run on deployment.

## 5. Verification and delivery

- **Unit tests:** current-salary selection across dated records, salary validation and correction, import row classification, and report calculations by currency.
- **API/database tests:** employee create and version conflict, salary audit events, invalid CSV rows, stale preview, atomic import failure, repeated commit, expired preview, and unauthorized access.
- **Browser tests:** search and open an employee, record a salary change, preview and commit a CSV, and read a filtered report; include loading and error recovery checks.
- **Scale check:** seed 10,000 synthetic employees, confirm stable pagination and usable report response times, and inspect query plans for the indexed paths.
- **Delivery:** deploy the application, provide setup instructions and a video demo, and keep incremental commits. Before implementing each feature, write a concise reviewed feature spec under `spec/`; record changes in `spec/ChangeLog.md`.

## Assumptions and tradeoffs

Annual base salary is the only compensation component. Reports use current values, not historical organization-wide snapshots. Imports accept CSV, update existing employees by code, and never delete employees absent from a file. Payroll, tax, bonuses, conversion between currencies, free-form AI questions, native Excel upload, and multi-role approvals remain outside the first release. A single-origin modular monolith keeps deployment and authorization simple while leaving the modules separable if scale or product scope later requires it.
