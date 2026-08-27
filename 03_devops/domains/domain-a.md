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
`04_tests/UCxx/evidence` directory. Do not call a MockMvc/H2 run E2E.

## Evidence rule

Each UC report links the exact command, exit status, raw report/log,
`result-summary.json`, and (for browser runs) the Playwright report plus any
failure screenshots. Missing Compose/MySQL or browser runs are recorded as
`NOT_RUN` or `E2E_PENDING`, never as a pass.

## A0 audit result

The existing Domain-A tests were reviewed for identical methods. No safe
duplicate deletion was identified; valid assertions were retained. The shared
JUnit tags and this domain command boundary were added without creating one
workflow per UC. UC-specific integration/E2E evidence is added in each
independent UC branch.
