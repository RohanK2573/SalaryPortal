# Employee and Salary Management Spec

**Status:** Draft for review  
**Depends on:** [Foundation](./specs-01-backend-foundation.md), [Authentication](./specs-02-hr-authentication.md), and [LLD](../docs/low-level-design.md)

## Why and what

HR needs one dependable place to find employees, maintain their details, and record salary changes without losing the history used to explain current pay.

## Scope and interface

- `GET /api/v1/employees` searches name or employee code and filters by country, department, and active/inactive status. It returns stable, paginated results with current salary. `GET /api/v1/employees/{id}` returns one employee and current salary.
- `POST /api/v1/employees` creates an employee and initial salary in one transaction. Required fields are unique employee code, name, country, department, job title, status, positive annual base amount, ISO currency, and effective date on or before today.
- `PATCH /api/v1/employees/{id}` changes profile fields using an employee version. Employee code is immutable. An inactive employee remains searchable but is excluded from current pay reports; hard deletion is unavailable.
- `GET /api/v1/employees/{id}/salary-records` returns history newest first. `POST` on that collection adds a dated salary record. The current salary is the latest record effective on or before the server's UTC date. An employee cannot have two records on the same effective date.
- `PATCH /api/v1/employees/{id}/salary-records/{recordId}` corrects amount, currency, or effective date using the salary-record version and a required reason. Corrections must preserve uniqueness and cannot use future dates. Creation and correction write actor, source, and before/after values to the audit trail.

## Decisions and non-goals

Annual base pay is stored as an exact decimal and exposed as a string. Currency belongs to each salary record, so a country can have more than one pay currency. Optimistic versions return `409` on stale edits. Payroll calculations, bonuses, future scheduled changes, and deleting salary records are outside scope.

## Acceptance scenarios

- Creating an employee saves both employee and initial salary or saves neither; duplicate employee codes fail clearly.
- Search, filters, and pagination produce stable results with 10,000 employees. Inactive employees remain visible but do not appear in pay reports.
- Backdated records preserve history; the latest eligible date determines current salary. Future and duplicate dates are rejected.
- Stale profile or salary corrections return `409`, and every successful salary change or correction has an audit event.
