# Domain D test and evidence operations

## Local Backend Verification

From the repository root:

```powershell
Set-Location backend
mvn -B --no-transfer-progress "-Dgroups=DOMAIN_D" clean verify
Set-Location ..
powershell.exe -NoProfile -File scripts/test/summarize-domain-d-surefire.ps1 `
  -ReportsDirectory backend/target/surefire-reports `
  -OutputDirectory backend/target/domain-d-report
```

The tag selects all nine current Domain-D classes. The summarizer parses Surefire XML with an XML parser, reports each class and count, and exits nonzero for missing suites, failures, or errors.

## Evidence Checks

```powershell
node 04_tests/verify-uc16-uc20-evidence.mjs
node 04_tests/domains/D-secondhand/verify-evidence-manifest.mjs
```

The first command validates the existing screenshot walkthrough. The second validates the Domain-D status boundary, committed backend reports, and the five pending real E2E mappings.

## Frontend Builds

```powershell
Set-Location frontend
npm ci
npm run build:mock
npm run build:real
```

`build:mock` only reproduces the UI walkthrough. `build:real` verifies the production API mode compiles; neither command is a full-stack E2E result.

## Real Compose Playwright

The repository has one shared config at `frontend/playwright.config.ts`. Once a Domain-D spec is added in its UC-specific PR, run it against the real Nginx/backend/MySQL stack:

```powershell
.\scripts\e2e\run-compose-e2e.ps1 e2e/domain-d/uc16-product-management.spec.ts -ResetDatabase
```

Replace the spec path for UC17-UC20. The runner preserves Playwright reports, screenshots, traces, videos, and service logs under `04_tests/platform-e2e/evidence/` and returns the real Playwright exit code.

## CI Semantics

- `.github/workflows/uc16-uc20-secondhand.yml` runs tagged backend evidence, the mock UI walkthrough build, manifest checks, and an `artifact smoke` check.
- The workflow uploads logs and Surefire reports before enforcing the captured Maven and summary exit codes.
- `.github/workflows/ci-cd.yml` owns the shared real Compose Playwright job. Its deploy and release jobs depend on that E2E job.
- The Domain-D artifact bundle check verifies files can be packaged and opened. It is not Kubernetes CD and is not deployment evidence.

No personal credentials, tokens, production secrets, or private keys belong in these commands or evidence files.
