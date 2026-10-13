# Deploy to Render

The app is one Docker web service. Its image builds the React UI and Spring Boot API, then serves both from one origin. Render provides HTTPS; the app connects to Render Postgres over the private network.

## 1. Push the repository to GitHub

The Render service must build from the GitHub repository. From the repository root, stage only the application and Render setup files; do not use `git add .` because this checkout also has unrelated untracked files:

```sh
git add Dockerfile README.md src/main/resources/application.yml docs/render-deployment.md deploy/render-entrypoint.sh
git diff --cached --check
git commit -m "chore: prepare Docker app for Render deployment"
```

If this local repository has no remote configured yet:

```sh
git remote add origin https://github.com/RohanK2573/SalaryPortal.git
git push -u origin main
```

If `origin` already exists, check it with `git remote -v` and push with `git push -u origin main`.

## 2. Create the PostgreSQL database

In the Render Dashboard:

1. Select **New + → PostgreSQL**.
2. Set a name such as `salary-management-db`.
3. Select **Singapore**. The web service must use the same region.
4. Choose the database plan:
   - **Free** for a short-lived assessment demo. It expires after 30 days, has a 1 GB limit, and has no backups.
   - **Paid** if the database needs to persist beyond the demo or retain data. Check the current plan price and backup options in Render before creating it.
5. Create the database and wait until it is available.
6. Open its **Info** page and keep it open for the service setup. You will need the **Internal Database URL**, database name, username, and password. Do not put credentials in GitHub.

Render's free-service limits can change; check [Render's free-tier documentation](https://render.com/docs/free) before choosing Free.

## 3. Create the Docker web service

1. Select **New + → Web Service** and connect the `RohanK2573/SalaryPortal` repository.
2. Choose branch `main`.
3. Name it `salary-management` and set region to **Singapore**.
4. Choose **Docker** as the runtime. Use `./Dockerfile` and the repository root as the build context.
5. Choose **Free** for a short assessment demo or a paid compute plan if you need it to stay responsive. A free web service can spin down after 15 minutes idle; the first request afterward can take about a minute.
6. Set the health check path to `/actuator/health/readiness`.
7. Add the environment variables below before the first deploy. Render supplies `PORT` automatically; do not set a fixed port.

| Variable | Value |
| --- | --- |
| `DATABASE_URL` | Render Postgres **Internal Database URL** |
| `DATABASE_USERNAME` | Render Postgres username |
| `DATABASE_PASSWORD` | Render Postgres password; mark secret if the dashboard offers that option |
| `SESSION_COOKIE_SECURE` | `true` |
| `APP_BOOTSTRAP_HR_USER_ENABLED` | `true` for first deploy |
| `HR_INITIAL_EMAIL` | `hr@incubyte.com` |
| `HR_INITIAL_PASSWORD` | A strong unique password you create for the hosted demo; do not reuse `incubyte` |

The app permits anonymous access to the health endpoint, so Render can check readiness. Flyway creates the schema at startup.

## 4. Deploy and bootstrap the HR account

1. Create the web service and follow its **Events** and **Logs** pages until deployment completes and the readiness check passes.
2. Open `https://<your-service-name>.onrender.com/actuator/health/readiness`. It should return a healthy status.
3. Open `https://<your-service-name>.onrender.com/signin` and sign in with `HR_INITIAL_EMAIL` and `HR_INITIAL_PASSWORD`.
4. After confirming sign-in, set `APP_BOOTSTRAP_HR_USER_ENABLED` to `false` in the service's **Environment** page and save/redeploy. Keep the initial password stored securely; never commit it.

The bootstrap creates the initial HR account if none exists. On restarts it leaves the same account unchanged. If the database already has an account with a different email, startup fails rather than creating or replacing the account.

## 5. Smoke-check the app

- Sign in and sign out.
- Open the Employees page, create an employee, and view the Reports page.
- Check that `/actuator/health/readiness` remains healthy.
- Confirm the site uses HTTPS and that login remains active after navigation.

## Local Docker behavior

The Docker entrypoint converts Render's `postgresql://` connection string to the JDBC URL expected by Spring. Local Compose already supplies `jdbc:postgresql://`, so its URL remains unchanged. The app listens on Render's `PORT` when provided and defaults to port 8080 for local runs.
