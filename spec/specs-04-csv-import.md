# CSV Import Spec

**Status:** Draft for review  
**Depends on:** [Employee and Salary Management](./specs-03-employee-salary-management.md) and [LLD](../docs/low-level-design.md)

## Why and what

HR currently works in spreadsheets. A reviewed import moves employee and salary data into the app while preventing a partly applied or silently changed file.

## Scope and interface

- Provide a downloadable UTF-8 CSV template with `employee_code`, `full_name`, `country_code`, `department`, `job_title`, `status`, `annual_base_amount`, `currency_code`, and `effective_date`. Accept one header row, at most 10,000 data rows, and at most 10 MiB.
- `POST /api/v1/imports/preview` stages the file for the current HR user. `GET /api/v1/imports/{id}` returns summary counts and paged row actions/errors. Each row is `CREATE`, `UPDATE`, `NO_OP`, or `INVALID`.
- Validate required fields, salary and date rules, code formats, and duplicate employee codes within the file. Existing database employee codes are updates. An unchanged employee and salary is a no-op. A changed salary must use an effective date later than the employee's latest record; same-date corrections use the salary edit flow.
- `POST /api/v1/imports/{id}/commit` is enabled only when every row is valid. It rechecks that proposed creates are still absent and that existing employee and latest-salary versions match the preview. In one transaction it applies all rows and audit events, then stores a result summary. Any conflict returns `409` with no employee changes; a repeated successful commit returns the saved result.
- Preview ownership is checked on read and commit. Uncommitted previews expire after 24 hours and return `410`. Staged row contents are removed 24 hours after creation, while a committed batch keeps its result summary for repeat requests.

## Decisions and non-goals

Staging in PostgreSQL makes preview and commit stable across requests. All-or-nothing commit avoids a dataset whose failures are easy to miss. Missing employees are never deleted. Native `.xlsx`, partial success, background jobs, and salary corrections through CSV are outside scope.

## Acceptance scenarios

- Preview identifies malformed rows and in-file duplicates with row numbers; commit remains unavailable while errors exist.
- Existing IDs show updates, identical rows show no-op, and new IDs show creates. A changed salary creates history and an audit event.
- A stale preview changes no employee data. Repeating a committed request does not duplicate records; an expired or another user's preview cannot be committed.
