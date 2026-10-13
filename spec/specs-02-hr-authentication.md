# HR Authentication Spec

**Status:** Draft for review  
**Depends on:** [Backend Foundation](./specs-01-backend-foundation.md) and [LLD](../docs/low-level-design.md)

## Why and what

Salary data is sensitive. One authenticated HR user can access the assessment app; anonymous visitors cannot read employee data or call business APIs.

## Scope and interface

- Create one HR account through an idempotent bootstrap command. Email and initial password come from deployment secrets; store only a password hash. No credential is committed to the repository.
- `GET /api/v1/session` is public and returns authentication state. It also initializes a readable CSRF token cookie for the same-origin UI. `POST /api/v1/session` signs in with email and password; `DELETE /api/v1/session` signs out. Sign-in rotates the server-side session ID, and sign-out invalidates it.
- The session cookie is `HttpOnly` and `SameSite=Lax`, with `Secure` on HTTPS. Mutating requests, including sign-in, must send the CSRF token in an `X-XSRF-TOKEN` header. The CSRF token is separate from the session cookie.
- All employee, salary, import, and reporting endpoints require the HR session. Missing or expired sessions return `401`; authorization denial returns `403`. Responses containing salary data are not publicly cacheable.

## Decisions and non-goals

Server-side sessions fit the one-origin, single-application deployment and avoid browser-stored bearer tokens. Public registration, password reset, SSO, and multiple roles are outside the first release. Local HTTP development may omit the cookie's `Secure` attribute; deployed HTTPS must use it.

## Acceptance scenarios

- Anonymous requests to business APIs fail; valid sign-in grants access; sign-out and expired sessions remove access.
- Wrong credentials do not reveal whether an email exists. Session IDs change on sign-in, and CSRF-free mutations are rejected.
- The bootstrap command can be rerun without creating another HR account or logging the password.
