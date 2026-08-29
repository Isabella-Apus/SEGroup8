# Shared Playwright + Compose full-stack E2E

This is the single browser E2E entry point for UC01-UC25. Domain specs belong
under `frontend/e2e/domain-a/` through `frontend/e2e/domain-e/` and reuse the
shared `frontend/playwright.config.ts`, fixtures, and helpers.

The CI lifecycle starts MySQL, backend, and frontend once, runs the smoke gate
first, and then runs every Domain A-E Playwright suite against the same stack.
`container-acceptance/` retains the independently reproducible Compose
acceptance checks that were originally tracked as Issue #65.

## Local run

From the repository root, with Docker Desktop/Engine running:

```powershell
Copy-Item .env.docker.example .env
.\scripts\e2e\run-compose-e2e.ps1
```

The script validates Compose, builds the local development images, starts
`database` → `backend` → `frontend`, waits for Docker healthchecks and HTTP
health endpoints, then runs the real browser against the Compose frontend.
It does not use `dev:mock` or a mock API server.

Run one domain or one spec without changing the platform setup:

```powershell
.\scripts\e2e\run-compose-e2e.ps1 e2e/domain-a/
.\scripts\e2e\run-compose-e2e.ps1 e2e/domain-a/uc01-login.spec.ts
.\scripts\e2e\run-compose-e2e.ps1 --grep '@UC01'
```

Use `-KeepServices` while debugging, or `-ResetDatabase` when a clean seeded
database is required. The default cleanup removes containers but preserves the
named MySQL volume; reset is explicit.

## CI artifact flow

The CI `backend` job runs Maven tests and packages `target/app.jar` once. The
CI `frontend` job builds the real frontend once. The E2E job downloads those
tested/build artifacts and sets `COMPOSE_FILE=compose.yml:compose.e2e.yml`, so
Compose runs `backend/Dockerfile.runtime` and `frontend/Dockerfile.runtime`.
The E2E container stack therefore tests the same JAR and frontend build that
were produced by the earlier jobs; it does not compile Maven a second time.

## Evidence and failure diagnosis

Results are written to `04_tests/platform-e2e/evidence/`:

- `smoke/`: smoke Playwright JSON/HTML and failure artifacts
- `full/`: complete Domain A-E Playwright JSON/HTML and failure artifacts
- `logs/`: Compose status/config, per-service logs, startup-stage logs, and
  Playwright output

The final A-E + Playwright aggregate is generated under `.ci/pipeline-summary/`
and uploaded with the E2E evidence. It is intentionally not retained as a
root-level `04_tests` directory.

The script returns the real Playwright/Compose exit code. On failure it records
the failing stage and collects frontend, backend, database, and Compose logs.
CI uploads this directory and blocks deploy/release through the E2E job.

The container acceptance entry point is:

```powershell
powershell -ExecutionPolicy Bypass -File .\04_tests\platform-e2e\container-acceptance\collect-evidence.ps1
```

For an independently reviewed UC run, set `E2E_OUTPUT_DIR` before invoking the
same runner, for example
`$env:E2E_OUTPUT_DIR='04_tests/UC01/evidence'`. The runner preserves that
directory; when the variable is absent it uses the shared platform directory.

## Test data and credentials

Compose initializes MySQL from `backend/src/main/resources/schema.sql` and
`docker/mysql/02-seed.sql` on a fresh named volume. The shared test account is
provided through environment variables (`E2E_USERNAME`, `E2E_PASSWORD`, and
`E2E_ROLE`); do not commit personal credentials, tokens, or secrets.

Specs must use isolated or uniquely named data when they mutate state. Use
`-ResetDatabase` for local reruns that require the seed state. CI runs on a
fresh runner and uses the same deterministic seed path.

To stop and remove the Compose containers:

```powershell
docker compose down --remove-orphans
```

To also discard the seeded database volume, use the explicit reset option:

```powershell
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase
```
