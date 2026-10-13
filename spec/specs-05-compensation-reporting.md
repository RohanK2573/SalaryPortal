# Compensation Reporting Spec

**Status:** Approved
**Depends on:** [Employee and Salary Management](./specs-03-employee-salary-management.md) and [LLD](../docs/low-level-design.md)

## Why and what

Answer the assessment's two required HR questions from current gross annual base salary: minimum, maximum, and average in a country; and average for a job title within a country. Keep currency visible so results remain comparable.

## Scope and interface

- `GET /api/v1/reports/compensation` accepts `groupBy=country|countryJobTitle|countryDepartment` (default `country`) plus optional `countryCode` and `department` filters. `country` answers the first required question; `countryJobTitle` answers the second. `countryDepartment` is a secondary insight. An optional country filter can narrow the job-title or department view to one country.
- Return overall matching headcount and a `groups` list. Each group identifies its `countryCode`, `currencyCode`, and, when applicable, `jobTitle` or `department`. Each group has `headcount`, `minimum`, `maximum`, and `average` annual base salary as decimal strings; `total` and `median` are secondary values. There is no monetary total across currencies.
- Include only `ACTIVE` employees and each employee's latest salary record effective on or before the server's UTC date. Employees without a current record do not contribute. This active-only rule is a product assumption; inactive employees remain searchable in the directory.
- Group by currency **within** every country and job-title or department group. Validate the group option and filters using the shared API conventions; an unmatched filter returns zero headcount and an empty group list. Calculate from PostgreSQL rather than loading all employees or salary records into application memory.

## Decisions and trade-offs

Seed salaries use each employee's local currency. The employee API currently accepts any valid ISO currency code for a country, so a country may contain multiple currencies; those salaries produce separate report groups. Never convert or add different currencies. Average and even-count median use exact decimal arithmetic and round to four fractional digits with `HALF_UP`; minimum, maximum, and total preserve stored decimal precision. Historical organization-wide snapshots, custom formulas, exports, and natural-language questions are outside scope.

## Acceptance scenarios

- For three active employees in India paid INR 40, 60, and 80, the country group has headcount 3, minimum 40, maximum 80, and average 60. If the first and third have job title `Engineer`, their India/Engineer group has headcount 2 and average 60.
- An additional India employee paid USD produces a separate USD group and does not change INR metrics. The response has no combined monetary aggregate for India.
- Inactive employees, employees without a current record, and future-dated records never affect results. Filters with no match return zero headcount and no groups; invalid group options return `422`.
- Even and odd group sizes yield correct medians; country, country/job-title, and country/department views remain usable on the deterministic 10,000-employee dataset.
