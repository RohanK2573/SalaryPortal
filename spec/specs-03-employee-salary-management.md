# Employee and Salary Management Spec

**Status:** Approved
**Depends on:** [Foundation](./specs-01-backend-foundation.md), [Authentication](./specs-02-hr-authentication.md), and [LLD](../docs/low-level-design.md)

## Why and what

HR needs one dependable place to find employees, maintain their details, and record salary changes without losing the history used to explain current pay.

## Scope and interface

- `GET /api/v1/employees` searches name or employee code and filters by country, department, and active/inactive status. It returns stable, paginated results with current salary. `GET /api/v1/employees/{id}` returns one employee and current salary.
- `POST /api/v1/employees` creates an employee and initial salary in one transaction. Required fields are unique employee code, name, country, department, job title, status, positive annual base amount, ISO currency, and effective date on or before today.
- `PATCH /api/v1/employees/{id}` changes profile fields using an employee version. Employee code is immutable. An inactive employee remains searchable but is excluded from current pay reports; hard deletion is unavailable.
- `GET /api/v1/employees/{id}/salary-records` returns history newest first. `POST` on that collection adds a dated salary record. The current salary is the latest record effective on or before the server's UTC date. An employee cannot have two records on the same effective date.
- `PATCH /api/v1/employees/{id}/salary-records/{recordId}` corrects amount, currency, or effective date using the salary-record version and a required reason. Corrections must preserve uniqueness and cannot use future dates. Creation and correction write actor, source, and before/after values to the audit trail.

## Proposed API algorithms

**Rules shared by every endpoint:** Require the HR session; Spring Security returns `401` without it and checks CSRF on writes. Treat IDs as UUIDs and dates as `YYYY-MM-DD`. Read the current date from the injected UTC clock. Parse annual amounts from plain decimal strings into `BigDecimal` without floating-point conversion; require a positive value that fits `NUMERIC(19,4)`. Validate country as ISO 3166-1 alpha-2 and currency as ISO 4217. Return field-level `422` problems for invalid input, including malformed IDs; return `404` for an unknown employee or a salary record outside that employee, and `409` for a duplicate code/date or stale version. Database constraints remain the final guard against concurrent duplicates. Never expose SQL details.

### `GET /api/v1/employees`

1. Normalize optional `q`, `countryCode`, `department`, and `status` query parameters. Match trimmed `q` as a case-insensitive literal substring of employee code or name; compare country and status exactly after code normalization, and department case-insensitively. Combine filters using `AND`. Inactive employees remain in the directory.
2. Validate one-based `page` (default `1`), `size` (default `25`, maximum `100`), and one `sort` value such as `fullName,desc`. Allow fields `employeeCode`, `fullName`, `countryCode`, `department`, `jobTitle`, or `status`, with `asc` or `desc`. Default to `employeeCode,asc`; append `employeeCode,asc` to other sorts for stable ties. Reject unsupported sort fields with `422`; salary amount is excluded because currencies differ.
3. Query only the requested employee page and its total count. Fetch each page employee's latest salary record with `effectiveDate <= today`, choosing the greatest effective date, in one additional query rather than one query per employee.
4. Return `200` with `items`, `page`, `size`, and `total`. Each item includes employee identity/profile, `version`, and current salary (`null` only if legacy data has no eligible record). Stable ordering applies while the underlying data is unchanged.

### `GET /api/v1/employees/{id}`

1. Load the employee by UUID or return `404`.
2. Select that employee's latest salary record effective on or before today.
3. Return `200` with the profile, `version`, and current salary, including its record ID and version so the UI can open a correction safely. Inactive employees still show their salary history.

### `POST /api/v1/employees`

1. Validate and normalize all required profile fields and the initial salary. Trim the employee code and store its uppercase form as its stable key; treat codes that differ only by case as duplicates. Reject blank required fields, nonpositive/overprecision amounts, invalid ISO codes, and effective dates after today with `422`.
2. Start one service transaction. Insert the employee with a generated UUID and initial version, then insert the salary record with its own UUID/version and the same effective date supplied by HR.
3. Insert an immutable salary audit event with the HR actor, `MANUAL` source, empty before-state, and exact after-state. Commit all three rows together; any failure rolls them all back. Translate a concurrent unique employee-code violation to `409`.
4. Return `201` with `Location: /api/v1/employees/{id}` and the created employee representation.

### `PATCH /api/v1/employees/{id}`

1. Require the employee `version` and at least one mutable field. Permit name, country, department, job title, and status; reject a supplied employee code or explicit null/invalid field with `422`.
2. In one service transaction, load the employee or return `404`. Compare the supplied version with the stored version; return `409` if stale.
3. Apply validated fields and increment the version only when a value actually changes. Preserve employee code and salary records. Return `200` with the updated profile, version, and current salary. An inactive employee remains searchable but is omitted from compensation reports.

### `GET /api/v1/employees/{id}/salary-records`

1. Confirm the employee exists or return `404`.
2. Apply the shared one-based page/size limits. Query only that employee's records ordered by `effectiveDate DESC, id ASC`; include each record's ID, amount string, currency, date, and version.
3. Return `200` with `items`, `page`, `size`, and `total`. Backdated records remain visible even when they are not the current salary.

### `POST /api/v1/employees/{id}/salary-records`

1. Validate amount, currency, and effective date using the shared rules. Backdated dates are allowed; dates after today are `422`.
2. In one service transaction, confirm the employee exists, insert a new salary record, and insert an audit event with actor, `MANUAL` source, empty before-state, and exact after-state. Do not overwrite an earlier record.
3. Enforce unique `(employee_id, effective_date)` in PostgreSQL; return `409` if that date already exists, including when another request inserts it concurrently. A different date may record an unchanged amount.
4. Return `201` with the new record and its `Location`. Current salary is recalculated from the latest eligible effective date on later reads.

### `PATCH /api/v1/employees/{id}/salary-records/{recordId}`

1. Require the record `version`, a nonblank correction reason, and at least one of amount, currency, or effective date. Validate supplied values; a future date or a correction that leaves all values unchanged is `422`.
2. In one service transaction, load the record through the employee ID and record ID or return `404`. Compare the supplied version; return `409` if stale. Validate that a changed effective date does not collide with another record for this employee.
3. Capture exact before-values, apply the correction with optimistic locking, and insert an immutable audit event containing actor, `MANUAL` source, reason, before-values, and after-values. Roll back the correction if the audit insert fails. A concurrent version or unique-date conflict returns `409`.
4. Return `200` with the corrected record and new version. Current salary is determined again by effective date on the next read; corrections never delete history or prior audit events.

**Review decisions:** Employee codes are canonicalized to uppercase for matching across manual entry and CSV import. Profile PATCH requests with no actual change return the existing version. Directory sorting excludes salary amount because amounts in different currencies are not comparable. These choices are proposed here for approval before implementation.

## Decisions and non-goals

Annual base pay is stored as an exact decimal and exposed as a string. Currency belongs to each salary record, so a country can have more than one pay currency. Optimistic versions return `409` on stale edits. Payroll calculations, bonuses, future scheduled changes, and deleting salary records are outside scope.

## Acceptance scenarios

- Creating an employee saves both employee and initial salary or saves neither; duplicate employee codes fail clearly.
- Search, filters, and pagination produce stable results with 10,000 employees. Inactive employees remain visible but do not appear in pay reports.
- Backdated records preserve history; the latest eligible date determines current salary. Future and duplicate dates are rejected.
- Stale profile or salary corrections return `409`, and every successful salary change or correction has an audit event.
