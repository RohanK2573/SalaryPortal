# Frontend Foundation and Session Spec

**Status:** Approved  
**Depends on:** [Backend Foundation](./specs-01-backend-foundation.md), [HR Authentication](./specs-02-hr-authentication.md), [Seed and Deployment Readiness](./specs-06-seed-deployment-readiness.md), and [LLD](../docs/low-level-design.md)

## Why and what

Establish the React application, shared visual language, API client, routing, and minimal HR sign-in experience used by every product screen. The foundation must keep salary data on the server, handle session expiry consistently, and support a polished responsive interface without introducing a large UI framework.

## Scope and interface

- Create a React and TypeScript application with Vite and npm under `frontend/`. Commit `package-lock.json`. Use React Router for navigation, TanStack Query for server state, React Hook Form with Zod for forms, CSS variables and reusable local components for styling, and Lucide icons. Do not add a general-purpose component library or global client-state store.
- Organize frontend code into `app`, `api`, `components`, `auth`, `employees`, `reports`, and `imports`. Keep URL/query state in the router, server state in TanStack Query, and short-lived dialog/form state in its owning feature.
- Define typed client contracts for session state, employees, salary records, paged responses, problem responses, compensation groups, import batches, and import rows. Preserve salary amounts as decimal strings and format them without converting them to JavaScript floating-point numbers.
- Provide a calm professional design system with neutral surfaces, restrained blue accents, readable data density, consistent spacing, clear status colors, semantic HTML, visible keyboard focus, labelled controls, WCAG AA contrast, and responsive behavior from 360 px upward.
- Provide reusable button, input, select, badge, alert, dialog, skeleton, empty-state, table, pagination, and page-header components. Tables may scroll horizontally on small screens, but identity, status, and primary actions must remain understandable.

## Routes and navigation

- `/` sends an authenticated HR manager to `/employees` and an anonymous visitor to `/signin`.
- `/signin` is public. `/employees`, `/employees/new`, `/employees/:id`, `/reports`, `/imports`, and `/imports/:id` require an authenticated session.
- Unknown routes show an application-level not-found page. A protected-route redirect may carry only a validated internal `returnTo` path; external or protocol-relative values are rejected.
- The authenticated shell exposes Employees, Reports, and Import in that priority order, plus the signed-in email and Sign out. Desktop uses a compact sidebar or header; small screens use an accessible navigation menu.

## API and session algorithm

1. The shared API client uses relative `/api/v1` URLs, `credentials: "same-origin"`, JSON request/response handling, empty `204` handling, CSV download handling, and typed parsing of `application/problem+json`.
2. On initial application load, call `GET /api/v1/session`. This both initializes the `XSRF-TOKEN` cookie and determines whether the user is authenticated. Render a full-page loading state until the result is known.
3. Before `POST`, `PATCH`, or `DELETE`, read the `XSRF-TOKEN` cookie and send it in `X-XSRF-TOKEN`. Never persist the token outside browser cookie/request handling or log it.
4. Sign-in sends email and password to `POST /api/v1/session`. After success, call `GET /api/v1/session` again to receive the token associated with the authenticated session, clear the password from component state, and navigate to the safe return route or `/employees`.
5. Sign-out sends `DELETE /api/v1/session`, clears cached protected queries, and navigates to `/signin`.
6. Any protected request returning `401` clears authenticated state and protected caches, then redirects to sign-in with a short session-expired message. Feature screens retain control of `403`, `409`, `413`, `422`, and unexpected failures so they can offer specific recovery.
7. Do not send credentials, salary data, raw CSV contents, session cookies, or CSRF tokens to browser logs or third-party services.

## Screen states and copy

- Sign-in contains email, password, submit, and an inline error region. Invalid credentials use the backend's generic message; rate limiting explains that the user can retry later without revealing account existence.
- A backend outage preserves the entered email, clears the password, and offers Retry.
- Protected pages use shared loading, empty, recoverable-error, forbidden, and not-found patterns. Error alerts state what failed and the next useful action.
- Buttons and mutations expose pending state and prevent duplicate submission. Dialog focus is trapped while open and returns to the invoking control when closed.

## Decisions and non-goals

The custom design system keeps the assessment visually coherent without framework lock-in. Authentication is an existing implementation choice, so the UI includes only the minimum required sign-in and sign-out experience. Registration, password reset, account administration, multiple roles, dark mode, localization, offline use, client analytics, and storing salary data in browser persistence are outside scope.

## Acceptance scenarios

- An anonymous user can load the sign-in route, receive the CSRF cookie, sign in, and reach the employee directory. Invalid credentials and rate limits are understandable and do not expose account details.
- Refreshing a protected route restores a valid session. An expired session returns to sign-in, clears protected cache data, and can safely resume an internal route after a new sign-in.
- Sign-out invalidates the session and prevents protected data from remaining visible through navigation.
- Shared API handling correctly represents successful JSON, `204`, CSV, field errors, and non-field problems.
- Core navigation and dialogs work by keyboard, focus is visible, and the shell remains usable at 360 px, tablet, and desktop widths.
- Focused unit/component tests cover problem parsing, exact amount formatting, CSRF headers, protected routing, sign-in, sign-out, and session expiry.
