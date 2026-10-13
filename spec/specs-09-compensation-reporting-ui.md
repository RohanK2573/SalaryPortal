# Compensation Reporting UI Spec

**Status:** Approved  
**Depends on:** [Frontend Foundation](./specs-07-frontend-foundation.md), [Employee Management UI](./specs-08-employee-management-ui.md), [Compensation Reporting](./specs-05-compensation-reporting.md), and [LLD](../docs/low-level-design.md)

## Why and what

Make the assessment's two required salary questions answerable from exact, readable results: minimum, maximum, and average salary in a country; and average salary for a job title within a country. Currency separation must remain visible so the UI never implies that unlike currencies were combined.

## Primary reports

- `/reports` provides `By country` and `By job title` views. Preserve `view`, `countryCode`, and optional department filters in URL parameters.
- `By country` calls `GET /api/v1/reports/compensation?groupBy=country` with an optional country filter. Show country, currency, headcount, minimum, maximum, and average. When one country is selected, render a separate summary-card set for each returned currency.
- `By job title` requires a country selection in the UI and calls `groupBy=countryJobTitle&countryCode={code}`. Show job title, currency, headcount, and average as the primary columns; minimum and maximum remain supporting columns.
- All monetary fields remain decimal strings and display with ISO currency. Full country names may accompany ISO country codes. Never merge or total results across currencies.
- Explain concisely that reports contain active employees with a current salary and that currencies are not converted.

## Secondary insights

- Place `By department` under a clearly labelled More insights section and call `groupBy=countryDepartment` with the existing country and department filters.
- Total and median may appear as secondary table columns or details but must not receive stronger visual emphasis than the required minimum, maximum, and average values.
- Secondary insights must reuse the same currency separation, exact formatting, loading, empty, and error behavior.

## Interaction and states

- Use summary cards and responsive tables; do not add charts in this release. Exact answers take priority over decorative visualization.
- Keep current data visible during background refetch and show a subtle updating state. Changing filters supersedes obsolete requests so stale results are not labelled as current.
- An unmatched filter returns an informative empty state, not an error. Invalid input shows the backend field message. Network or server failure offers Retry and retains selected filters.
- Tables remain readable at small widths using controlled horizontal scrolling and a stable first identity column.

## Decisions and non-goals

Reports use database aggregates from the backend and never derive organization-wide results from employee pages. The UI requires a country for job-title reporting because that is the requested business question. Currency conversion, combined global monetary totals, historical trends, charts, exports, configurable formulas, saved reports, and natural-language questions are outside scope.

## Acceptance scenarios

- Country view directly shows minimum, maximum, and average by country and currency.
- Job-title view directly shows average salary for each title in the selected country, separated by currency.
- Multiple currencies within one country create visibly independent cards or rows and are never combined.
- Empty filters remain a successful state; invalid filters and request failures provide useful recovery.
- Active-only reporting and the absence of conversion are visible without overwhelming the page.
- Tests cover view/filter URL state, country selection, multiple currencies, empty results, exact decimal formatting, background refresh, and backend problems.
