# Employee Management UI Spec

**Status:** Approved  
**Depends on:** [Frontend Foundation](./specs-07-frontend-foundation.md), [Employee and Salary Management](./specs-03-employee-salary-management.md), and [LLD](../docs/low-level-design.md)

## Why and what

Give the HR manager a fast employee directory and dependable forms for maintaining employee profiles and current annual base salary. The experience must remain usable with 10,000 employees and make current salary, currency, effective date, and employment status unambiguous.

## Employee directory

- `/employees` is the authenticated landing page. Store `q`, `countryCode`, `department`, `status`, `page`, `size`, and `sort` in URL parameters so refresh and browser navigation preserve the view.
- Default to `page=1`, `size=25`, and `sort=employeeCode,asc`. Offer page sizes 25, 50, and 100. Debounce search by 300 ms; changing search, filter, sort, or size resets to page 1.
- Query the server for every view. Do not download all employees or perform client-side filtering. Show the current result range and total count.
- Search matches name or employee code. Filters cover country, exact department text, and active/inactive status. Sortable columns use only backend-supported fields.
- Desktop shows employee code, name, country, department, job title, status, and current salary in a data table. Small screens use employee cards. Name/row navigation opens `/employees/:id`.
- Display current salary as exact amount plus ISO currency. Show `Not recorded` when `currentSalary` is null. Status uses text and a badge rather than color alone.
- Keep the previous results visible during background refresh. Initial load uses a table-shaped skeleton. No matches show a Clear filters action; a genuinely empty database offers Create employee.

## Create employee

- `/employees/new` contains Employee details and Initial salary sections and submits `employeeCode`, `fullName`, `countryCode`, `department`, `jobTitle`, `status`, `annualBaseAmount`, `currencyCode`, and `effectiveDate` to `POST /api/v1/employees`.
- Employee code is presented as a permanent identifier and normalized visually to uppercase. Country and currency use accessible ISO option lists. Department and job title remain free text because the backend has no canonical lookup endpoint.
- Status defaults to `ACTIVE`. Effective date defaults to the current UTC date and may not be later. Salary accepts a positive plain decimal with at most four fractional digits; exponent notation is rejected.
- Client validation improves immediacy, while backend validation remains authoritative. Map backend field errors to controls, focus the first invalid field, and retain all entered values.
- A duplicate employee code (`409`) preserves the form and explains that a different code is required. Successful creation navigates to the employee detail route returned by the response.

## Employee detail and profile updates

- `/employees/:id` displays employee identity, profile, status, and current salary first, followed by supporting salary history.
- Employee code is never editable. Profile editing permits full name, country, department, job title, and status and sends only changed fields plus the current employee `version` to `PATCH /api/v1/employees/{id}`.
- Disable Save when no value changed. Explain that inactive employees remain searchable but do not contribute to compensation reports.
- Successful edits refresh the employee detail and relevant directory caches. A `409` preserves the draft and offers Review latest data; reload the server version before another save.
- A `404` shows that the employee is unavailable and links back to the directory. Network errors preserve drafts and allow retry.

## Salary history and maintenance

- Show current salary prominently with amount, currency, and effective date. Salary history is paginated at 25 records per page and ordered by the backend newest effective date first.
- Add salary collects `annualBaseAmount`, `currencyCode`, and `effectiveDate`, then sends `POST /api/v1/employees/{id}/salary-records`. Explain that this creates a dated record and preserves earlier records.
- Correct salary opens from an existing history row and sends the salary record `version`, a required correction `reason` of at most 500 characters, and at least one changed field to `PATCH /api/v1/employees/{id}/salary-records/{recordId}`.
- Corrections are disabled until a value actually changes and a reason is present. Duplicate dates and stale versions show conflict-specific guidance and preserve entered values.
- Successful additions or corrections refresh current salary and history. Prevent duplicate mutation submissions and keep dialogs keyboard accessible.

## Error and recovery rules

- `401` uses the shared session-expiry flow. `403` explains that the action is unavailable. `422` maps field errors and shows any remaining detail in the form alert.
- `409` never silently overwrites server data. Profile and salary conflict screens preserve the attempted values and require refresh/review before retry.
- Unexpected and network failures offer retry without discarding unsaved input. Cancel explicitly discards the current dialog draft after warning only when values changed.

## Decisions and non-goals

Employee creation uses a dedicated page; profile and salary edits use focused dialogs or panels on the detail page. Salary history is supporting information because the clarified assessment requires current salary only. Employee deletion, bulk actions, exports, avatars, organization charts, payroll components, scheduled future salary, and client-side country-to-currency enforcement are outside scope.

## Acceptance scenarios

- Search, filters, supported sorting, and server pagination remain stable and responsive with the 10,000-employee seed; URL state survives refresh and back/forward navigation.
- Creation submits the exact backend contract, preserves invalid input, reports duplicate codes, and opens the new employee after success.
- Profile updates include the current version, avoid no-op submission, and recover from stale data without losing the draft.
- Salary addition and correction respect decimal/date rules, refresh the current salary, preserve history, and handle duplicate dates and stale versions.
- Inactive employees remain visible in the directory and are clearly marked as excluded from reports.
- Component tests cover query parameters, loading and empty states, create validation, profile conflicts, salary addition, salary correction, and session expiry.
