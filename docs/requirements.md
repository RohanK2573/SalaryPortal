# Salary Management Requirements

**Status:** Baseline scope for the assessment  
**Source:** `Salary Management Assessment- Candidates.docx`  
**Primary user:** ACME HR manager

## Goal

Replace spreadsheet-based salary management with a web application for an organization of 10,000 employees across multiple countries. The HR manager must be able to maintain salary data and answer common questions about how the organization pays people.

## Features in scope

1. **Employee directory:** Search, sort, filter, and page through employees. Show employee ID, name, country, department, role, employment status, and current salary.
2. **Salary management:** Create or update employees and record annual base salary in local currency with an effective date. Show previous salary records so a change does not erase history.
3. **Spreadsheet intake:** Provide a CSV template and an import preview that identifies invalid rows and duplicate employee IDs before committing changes.
4. **Pay reporting:** Show headcount, total annual base salary, average salary, and median salary by country and department, with filters and separate totals for each currency.
5. **Access:** Restrict employee and salary data to an authenticated HR user. Use synthetic data for the assessment demo.

## Quality and delivery criteria

- Build a functional UI and backend backed by a relational database. Seed 10,000 synthetic employees through a repeatable script; search and reports must remain usable at that scale.
- Validate inputs on the server, use exact decimal values for money, and retain an audit trail of salary changes.
- Include meaningful, fast, deterministic unit tests for core rules and tests for key API and import flows. Keep the code readable and maintainable.
- Deploy the application, record a video demo, and provide setup instructions and relevant design notes. Make incremental Git commits that show the solution's evolution.

## Deliberately excluded

- **Payroll payments, taxes, bonuses, equity, and benefits:** Their business rules are not defined in the assessment; annual base salary provides a complete core workflow.
- **Currency conversion:** No exchange-rate source or date policy is specified. Reports will not add amounts from different currencies together.
- **Open-ended AI questions:** Defined, filterable reports provide answers whose calculations can be checked.
- **Native Excel upload and multi-role approvals:** CSV intake and one HR role keep the first release focused while covering the primary user journey.

## Assumptions to revisit if clarified

The assessment does not prescribe salary components, report metrics, import format, exchange-rate policy, or access roles. The choices above are our proposed product scope, not additional requirements imposed by the assessment.
