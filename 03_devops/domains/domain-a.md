# Domain-A identity/governance test entry

This is the authoritative local entry point for the shared Domain-A test
boundary covering UC01-UC05 and reusable platform security behavior.

## Test scope

- `DOMAIN_A`: identity, profile/address, merchant application, user governance,
  report/block/credit business tests.
- `PLATFORM`: reusable JWT/interceptor/security-contract behavior. These tests
  are reported separately and are not counted as personal UC business work.
- Existing Mockito and standalone MockMvc tests remain valid unit/API evidence;
  they do not replace real database integration or browser E2E evidence.

## Local commands

From the repository root:

```bash
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
mvn -B -f backend/pom.xml -Dgroups=PLATFORM test
mvn -B -f backend/pom.xml clean verify
cd frontend && npm ci && npm run build:real
```

Real Compose/browser evidence is collected with:

```powershell
.\scripts\e2e\run-compose-e2e.ps1 -KeepServices
```

Set `E2E_OUTPUT_DIR` to route a run to its UC-specific
`04_tests/UCxx/evidence` directory; the runner now preserves this override.
Without it, CI writes the combined five-UC result to
`04_tests/platform-e2e/evidence`. Do not call a MockMvc/H2 run E2E.

## Evidence rule

Each UC report links the exact command, exit status, raw report/log,
`result-summary.json`, and (for browser runs) the Playwright report plus any
failure screenshots. Missing Compose/MySQL or browser runs are recorded as
`NOT_RUN` or `E2E_PENDING`, never as a pass.

## CI entry

All pull requests and pushes to `main` use the single workflow:

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

The backend job runs `-Dgroups=DOMAIN_A` first and then runs the full
`clean verify` regression. The same workflow also builds the real frontend and
runs the shared Compose + MySQL + Playwright job. It is one domain entry, not
five UC workflows.

## Verified locally on 2026-08-27

| Command | Result | Evidence boundary |
|---|---|---|
| `mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test` | A0 baseline PASS, 33 tests, 0 failures, 0 errors；五个 UC 合并后的最终本地聚合 PASS, 65 tests | H2/MockMvc Domain-A aggregate |
| `mvn -B -f backend/pom.xml clean verify` | A0 baseline PASS, 95 tests, 0 failures, 0 errors；五个 UC 合并后的最终本地回归 PASS, 127 tests | Full backend regression |
| `mvn -B -f microservices/pom.xml test` | PASS, security-contract 5 tests | Shared JWT contract, reported as PLATFORM/global |
| `npm ci` | PASS, 96 packages installed | Locked frontend dependencies |
| `npm run build:real` | PASS, 2421 modules built | Real frontend production build |
| `docker compose -f compose.yml -f compose.e2e.yml config --quiet` | PASS | Compose syntax only |
| `scripts/e2e/run-compose-e2e.ps1` | NOT_RUN: Docker Linux daemon unavailable | No MySQL/browser pass claimed |

The local Maven result above is the authoritative run record; CI must still
produce a GitHub Actions run before the CI gate itself can be called passed.

## A0 audit result

The existing Domain-A tests were reviewed for identical methods. No safe
duplicate deletion was identified; valid assertions were retained. The shared
JUnit tags and this domain command boundary were added without creating one
workflow per UC. UC-specific integration/E2E evidence is added in each
independent UC branch.
