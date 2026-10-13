# HR Authentication Spec

**Status:** Approved
**Depends on:** [Backend Foundation](./specs-01-backend-foundation.md) and [LLD](../docs/low-level-design.md)

## Why and what

Salary data is sensitive. One authenticated HR user can access the assessment app; anonymous visitors cannot read employee data or call business APIs.

## Scope and interface

- Bootstrap exactly one HR account. Normalize the email by trimming and lowercasing it; store only a password hash. The email and initial password come from deployment secrets and are never logged or committed. Rerunning bootstrap with the same email leaves the account and password unchanged; a different email fails clearly without creating another account. Password reset is a separate, out-of-scope operation.
- `GET /api/v1/session` is public, reports authentication state, and issues a readable CSRF token cookie before sign-in and after sign-in, session expiry, or sign-out. `POST /api/v1/session` signs in with email and password; `DELETE /api/v1/session` signs out. Sign-in rotates the server-side session ID; sign-out invalidates it. Sessions expire after 30 minutes of inactivity.
- The session cookie is `HttpOnly` and `SameSite=Lax`, with `Secure` on HTTPS. Every mutating request, including sign-in and sign-out, must send the separate CSRF token in an `X-XSRF-TOKEN` header. Invalid or missing tokens return `403`.
- All employee, salary, import, and reporting endpoints require the HR session. Missing or expired sessions return `401`; authorization denial returns `403`. Session and protected responses use `Cache-Control: no-store`.
- Limit sign-in attempts to 10 per five minutes per client IP, using only trusted proxy headers when deployed behind a proxy. Return a generic `429` problem response when the limit is reached; failed sign-in responses must not reveal whether the email exists.

## Decisions and non-goals

Server-side sessions fit the one-origin, single-application deployment and avoid browser-stored bearer tokens. The assessment names an HR manager but does not prescribe an authentication mechanism; the single-account model and 30-minute idle timeout are first-release design choices. Public registration, password reset, SSO, and multiple roles are outside the first release. Local HTTP development may omit the cookie's `Secure` attribute; deployed HTTPS must use it.

## Acceptance scenarios

- Anonymous requests to business APIs fail; valid sign-in grants access; sign-out and 30 minutes of inactivity remove access. Public `GET /session` supplies a CSRF token before and after sign-in, and after expiry or sign-out.
- Unknown email and wrong password produce the same response. Session IDs change on sign-in; mutations without a valid CSRF token return `403`; excessive sign-in attempts return a generic `429`.
- Bootstrap creates one account, is safe to rerun for the same normalized email without changing its password, rejects a different email, and never logs credentials.
