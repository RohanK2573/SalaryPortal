# Backend Spec Order

Review and approve these specs in order before implementing backend features. Shared conventions are defined first; each following feature depends on the earlier foundations.

| Order | Spec | Purpose | Depends on |
| --- | --- | --- | --- |
| 1 | [Backend Foundation](./specs-01-backend-foundation.md) | Application structure, database migrations, API conventions, and test setup. | Requirements and LLD (approved) |
| 2 | [HR Authentication](./specs-02-hr-authentication.md) | Seeded HR account, sessions, access control, and CSRF. | Backend Foundation (approved) |
| 3 | [Employee and Salary Management](./specs-03-employee-salary-management.md) | Employee directory, profile edits, salary history, and audit events. | Foundation and Authentication |
| 4 | [CSV Import](./specs-04-csv-import.md) | Staged preview and atomic import of employee and salary data. | Employee and Salary Management |
| 5 | [Compensation Reporting](./specs-05-compensation-reporting.md) | Current pay metrics grouped by country or department and currency. | Employee and Salary Management |
| 6 | [Seed and Deployment Readiness](./specs-06-seed-deployment-readiness.md) | Repeatable 10,000-employee seed, runtime configuration, and deployment checks. | All earlier backend specs |
