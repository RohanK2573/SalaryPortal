# Compensation Reporting Spec

**Status:** Draft for review  
**Depends on:** [Employee and Salary Management](./specs-03-employee-salary-management.md) and [LLD](../docs/low-level-design.md)

## Why and what

Give HR verifiable answers to common pay questions without combining different currencies or relying on generated explanations.

## Scope and interface

- `GET /api/v1/reports/compensation` accepts `groupBy=country|department` (default `country`) and optional country and department filters. It returns the headcount of active employees with a current salary plus groups identified by group value and currency.
- Each group returns headcount, total annual base salary, average, and median. Amounts are decimal strings. Median is the middle salary for an odd count and the mean of the two middle salaries for an even count.
- Include only `ACTIVE` employees and each employee's latest salary record effective on or before the server's UTC date. Employees with no eligible salary do not contribute to pay metrics. Empty filters return an empty group list and zero headcount.
- Query PostgreSQL for aggregation; do not fetch all salary records into application memory. Index the current-salary lookup and filter fields, and keep results separate by currency even within one country or department.

## Decisions and non-goals

Defined metrics can be tested and explained to HR. The first release reports current salary only. Currency conversion, historical organization-wide snapshots, custom formulas, exports, and natural-language questions are outside scope.

## Acceptance scenarios

- Even and odd group sizes return correct medians, and totals and averages use exact decimal arithmetic.
- Inactive employees and future-dated records never affect current results. Mixed-currency data produces separate groups and no combined monetary total.
- Filters, empty results, and both grouping modes work on the deterministic 10,000-employee dataset with usable response time.
