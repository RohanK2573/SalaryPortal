# CSV Import Spec

**Status:** Approved

**Depends on:** [Employee and Salary Management](./specs-03-employee-salary-management.md) and [LLD](../docs/low-level-design.md)

## Why and what

HR works in spreadsheets. Import must show the effect of a CSV before any employee data changes, then apply the reviewed file as one unit.

## Scope and interface

- `GET /api/v1/imports/template` downloads a UTF-8 CSV with this header: `employee_code,full_name,country_code,department,job_title,status,annual_base_amount,currency_code,effective_date`.
- `POST /api/v1/imports/preview` accepts a `multipart/form-data` part named `file`. The file must be UTF-8 CSV with one header, those nine columns, 1–10,000 data rows, and a size of at most 10 MiB. Column order may vary; missing, duplicate, or unknown columns, invalid CSV syntax or encoding, and an empty file return `422`. An oversized file returns `413`. No employee data changes during preview.
- A successful preview returns `201` with batch `id`, `status: PREVIEWED`, `expiresAt`, and counts for `CREATE`, `UPDATE`, `NO_OP`, and `INVALID`. `GET /api/v1/imports/{id}?page=1&size=25` returns those counts and paged rows (`items`, `page`, `size`, `total`); the maximum page size is 100. Each row includes its CSV record number (header is 1), employee code, proposed action, proposed profile and salary changes, and field-level errors. Invalid rows remain visible for correction in the source file.
- `POST /api/v1/imports/{id}/commit` returns the saved result counts and batch status `COMMITTED`. It is available only for a `PREVIEWED` batch with zero invalid rows. A repeat request for a committed batch returns the same result without applying rows again.
- All endpoints require the HR session; writes require CSRF protection. Only the HR user who created a batch may read or commit it. Return `404` for an unknown or another user's batch, `410` for an expired uncommitted preview, `422` for a commit containing invalid rows, and `409` when data changed since preview. Use the shared problem response with row and field errors; never return raw CSV or salary values in logs.

## Preview and commit rules

1. Parse quoted CSV fields and normalize values using Spec-03 rules: trim text, uppercase employee codes, validate `ACTIVE`/`INACTIVE`, ISO country and currency codes, plain positive decimal amounts fitting `NUMERIC(19,4)`, and `YYYY-MM-DD` effective dates no later than the server's UTC date. Require every field. Mark malformed row values and employee codes repeated within the file `INVALID`, with errors on each affected row. The database's canonical employee code determines whether a row targets an existing employee.
2. Classify a new code as `CREATE`. For an existing employee, compare the normalized profile and the latest salary record's amount, currency, and effective date. Identical values are `NO_OP`; any permitted difference is `UPDATE`. A proposed new salary record, including one with unchanged amount on a later effective date, must have a date later than the latest salary record. A same-date change or an earlier date is `INVALID`; salary corrections use the Spec-03 correction API. A profile-only update may keep the latest salary values and date.
3. Store the normalized rows and proposed changes in PostgreSQL with a file hash, owner, creation and expiry times, employee version, and latest salary record ID and version for every existing employee, including `NO_OP` rows. The preview is a snapshot for review, not a reservation of employee codes.
4. In one database transaction, lock the batch and reject invalid or expired previews. Lock affected employee rows in employee-code order, then recheck that every proposed create is still absent and that every existing employee and latest salary record still match the previewed identity and versions. Salary-record creation through Spec-03 must acquire the same employee lock before checking or adding history, so a concurrent append cannot bypass this recheck. If any check fails, return `409`, apply no rows, and require a fresh preview.
5. Apply all valid rows in that transaction: create new employees with initial salary; update changed profile fields and increment their versions; append later-dated salary records where proposed; leave `NO_OP` rows untouched. Each created salary record gets an immutable audit event with the HR actor, `CSV_IMPORT` source, and the batch ID. Save result counts and mark the batch `COMMITTED` in the same transaction. Database uniqueness and optimistic-lock conflicts also roll back the entire commit and return `409`.
6. An uncommitted batch expires 24 hours after creation and cannot be committed. Cleanup removes staged row contents after 24 hours; committed batches retain their result summary so repeat commits remain idempotent after cleanup. After cleanup, `GET /api/v1/imports/{id}` returns the summary with no row details.

## Decisions and non-goals

PostgreSQL staging preserves review across requests. Atomic commit prevents partial imports. Missing employees are never deleted. Native `.xlsx`, partial success, background processing, and salary corrections through CSV are outside scope.

## Acceptance scenarios

- The template and a valid file preview with accurate counts and row changes. Invalid headers or oversized files fail without staging employee changes; malformed values and in-file duplicates show row numbers and block commit.
- New codes create employees; existing codes update permitted fields; identical rows do nothing. A later-dated salary adds history and a `CSV_IMPORT` audit event. Same-date salary changes are rejected.
- A concurrent employee edit, salary change, or competing create returns `409` and leaves all employee and audit data unchanged. A repeated successful commit returns its saved result without duplicates.
- An expired preview returns `410`; a different HR user cannot read or commit another user's batch. A committed batch still returns its result after staged rows are cleaned up.
