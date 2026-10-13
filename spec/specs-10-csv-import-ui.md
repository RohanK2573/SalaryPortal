# CSV Import UI Spec

**Status:** Approved  
**Depends on:** [Frontend Foundation](./specs-07-frontend-foundation.md), [Employee Management UI](./specs-08-employee-management-ui.md), [CSV Import](./specs-04-csv-import.md), and [LLD](../docs/low-level-design.md)

## Why and what

Expose the completed CSV backend as a supporting workflow that shows HR every proposed change before employee data is modified. Import must clearly separate file selection, non-mutating preview, and explicit atomic commit.

## Prepare and upload

- `/imports` explains the expected UTF-8 CSV format, 10 MiB maximum, 1–10,000 data-row limit, and required columns without presenting CSV as the primary employee workflow.
- Download template calls `GET /api/v1/imports/template` and saves the server-provided filename/content.
- Use an accessible file picker for one `.csv` file. Client file-type and size checks provide immediate guidance; server validation remains authoritative.
- Preview sends `multipart/form-data` with a single part named `file` to `POST /api/v1/imports/preview`. Do not manually set the multipart boundary.
- On success, navigate to `/imports/{id}`. Uploading a preview never claims that employee data has changed.

## Preview experience

- Fetch the batch and paged rows from `GET /api/v1/imports/{id}`. Show total, Create, Update, No change, and Invalid counts and the preview expiry time.
- Display row number, employee code, proposed action, normalized employee/salary values, and row field errors. Use text labels and badges for `CREATE`, `UPDATE`, `NO_OP`, and `INVALID`; color is supplementary.
- Keep backend pagination, use a default size of 25, and allow 25, 50, and 100. Invalid rows stay visible and explain that the source CSV must be corrected and uploaded again.
- Enable Commit only when the batch is `PREVIEWED` and `invalidCount` is zero. A confirmation dialog repeats create, update, and no-change counts and states that the valid file will be applied as one transaction.

## Commit and recovery

- Commit calls `POST /api/v1/imports/{id}/commit`, disables repeat interaction while pending, and then shows immutable committed counts plus links to the employee directory and reports.
- Reopening or recommitting a `COMMITTED` batch displays the saved result without suggesting that changes were applied twice.
- `409` means employee data changed after preview: disable the old commit and offer Upload fresh CSV. `410` explains the 24-hour expiry and also restarts upload.
- `413` states the 10 MiB maximum. File-level `422` errors appear near the file control. A successful preview containing invalid rows remains a preview state rather than a request failure.
- Unknown or another user's batch uses the not-found state. Network failure offers retry without claiming an unknown commit outcome; refetch the batch before offering commit again.

## Decisions and non-goals

Import appears after Employees and Reports in navigation. A native accessible file picker is sufficient; no drag-and-drop dependency is required. Browser cell editing, partial commit, background processing, Excel support, import-history listing, removal of employees absent from the file, and displaying the original raw file are outside scope.

## Acceptance scenarios

- The template downloads correctly and a valid CSV produces a preview without changing employee data.
- Counts, normalized rows, actions, row numbers, and field errors match the backend response.
- Invalid rows clearly block commit and point HR back to the source file.
- A valid preview requires confirmation, commits once, and displays the saved result on repeat access.
- Stale, expired, oversized, malformed, unknown, and network-failure cases each provide a specific recovery path.
- Tests cover template download, multipart preview, invalid rows, paged results, commit confirmation, successful/idempotent commit, conflict, expiry, and oversized files.
