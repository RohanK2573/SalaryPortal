# Backend Spec Order

Review and approve each draft spec before implementing its feature. The sequence records implementation dependencies, not assessment priority: authentication, history, and CSV import are our additions; current employee salary and the two required report questions are the product priority after the assessment clarification.

| Order | Spec | Purpose | Depends on |
| --- | --- | --- | --- |
| 1 | [Backend Foundation](./specs-01-backend-foundation.md) | Application structure, database migrations, API conventions, and test setup. | Requirements and LLD (approved) |
| 2 | [HR Authentication](./specs-02-hr-authentication.md) | Additional choice: seeded HR account, sessions, access control, and CSRF. | Backend Foundation (approved) |
| 3 | [Employee and Salary Management](./specs-03-employee-salary-management.md) | Required employee and current salary management, plus additional history and audit events. | Foundation and Authentication (approved) |
| 4 | [CSV Import](./specs-04-csv-import.md) | Additional choice: staged preview and atomic import of employee and salary data. | Employee and Salary Management |
| 5 | [Compensation Reporting](./specs-05-compensation-reporting.md) | Required country min/max/average and country/job-title average, separated by currency; secondary insights. | Employee and Salary Management |
| 6 | [Seed and Deployment Readiness](./specs-06-seed-deployment-readiness.md) | Repeatable 10,000-employee seed, runtime configuration, and deployment checks. | All earlier backend specs |
