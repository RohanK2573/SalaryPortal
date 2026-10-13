# Salary Management Requirements

**Status:** Updated after assessment team clarification

**Sources:** `Salary Management Assessment- Candidates.docx` and the assessment team's email clarification

**Primary user:** One ACME HR manager

## Goal and required scope

Replace spreadsheet-based salary management with a usable web application for 10,000 employees across multiple countries. Seed employee data for the demo. The HR manager can manage employee records in the application, see each employee's **current gross annual base salary** in their local currency, and answer these questions:

1. What are the minimum, maximum, and average current salaries in a country?
2. What is the average current salary for a job title within a country?

Bonuses, allowances, deductions, payroll, and currency conversion are outside the requested scope. Excel import and export, salary history, and authentication are not required by the assessment clarification.

## Delivery and quality

Provide a functional frontend and backend backed by a relational database, seed 10,000 synthetic employees, keep the employee and report views usable at that scale, include meaningful deterministic tests, deploy the application, document setup and assumptions, record a video demo, and make incremental Git commits.

## Additional choices already built

- **Dated salary records and audit events:** Preserve changes and corrections while displaying the latest eligible record as current salary. This adds storage and workflow complexity; organization-wide historical reporting is not planned.
- **Single HR account and session authentication:** Protect salary data in the deployed demo. Registration and multiple roles are not planned; authentication is an implementation choice, not an assessment requirement.
- **CSV preview and import:** Support spreadsheet-based data entry with validation before commit. The required 10,000-employee demo dataset still comes from a repeatable seed; Excel file support is not planned.
- **Department, total, and median insights:** Useful secondary context for HR. The two requested report questions take priority in the UI, API, and demo.

## Assumptions and trade-offs

Reports use current salary for `ACTIVE` employees with a salary record; inactive employees remain searchable. Country and country–job-title monetary results are separated by currency, even within one country, because adding different currencies would be misleading. Seed data uses each employee's local currency. The current API validates ISO currency codes but does not enforce a country-to-currency mapping, so manual entry can create a different currency within a country; the UI must show currency and the reports must keep such amounts separate. No exchange rates or converted global totals are provided.
