# Domain B Test Entry

## Scope

The Maven reactor contains `catalog-service`, `shop-service`, `risk-service`,
and `behavior-service`. The Domain B count includes only tests selected by the
`DOMAIN_B` tag. Platform backend regression is reported separately and must
not be added to the Domain B integration count.

## Local commands

Run the tagged API and database integration suite:

```powershell
./04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Integration
```

Build the real frontend, start the Compose stack, run the browser suite, and
stop the stack:

```powershell
./04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser
```

Run both layers, including Compose startup and shutdown:

```powershell
./04_tests/domains/B-catalog-shop/run-domain-b.ps1 -All
```

The direct equivalents are:

```text
mvn -B -f microservices/pom.xml -Pdomain-b clean test
cd frontend && npm run build:real
docker compose up --build --wait
cd frontend && npm run e2e:domain-b
```

## Evidence

Surefire XML/TXT files are copied to
`04_tests/domains/B-catalog-shop/evidence/raw-reports/`. Playwright JUnit,
HTML, traces, videos, and failure screenshots are generated under the frontend
test output directories and uploaded by CI as a separate artifact.

Screenshots are supplementary review material. A screenshot is not evidence
that an automated test passed; the raw Surefire or Playwright report is the
authoritative result.

CI uses one workflow, `.github/workflows/domain-b-tests.yml`, with separate
`api-integration` and `browser-e2e` jobs. The jobs intentionally do not combine
MockMvc results with browser E2E results.
