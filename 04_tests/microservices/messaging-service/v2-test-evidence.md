# MS-06 V2 test evidence

Execution date: 2026-08-30  
Runtime: Java 21.0.8

| Command / suite | Result | Passed | Failed | Skipped | Notes |
|---|---:|---:|---:|---:|---|
| `mvn -B -f microservices/pom.xml -pl messaging-service -am clean verify` | PASS | 25 | 0 | 1 | 5 security-contract tests and 20 enabled Messaging tests; the conditional MySQL test is run separately |
| `mvn -B -f microservices/pom.xml clean test` | PASS | 45 | 0 | 1 | Full reactor: security-contract, catalog, shop, risk, behavior, messaging |
| `MySqlMigrationTest` with local MySQL enablement | PASS | 1 | 0 | 0 | MySQL 8.0, both Flyway versions, V2 tables/state/unique constraints, foreign-schema permission denial |
| V2 backend producer/transaction suite | PASS | 25 | 0 | 0 | Seven selected producer unit/integration classes, including unreachable-Messaging payment isolation |
| `mvn -B -f backend/pom.xml test` | FAIL | 175 | 11 | 0 | 194 total: 8 Testcontainers errors because Docker was unavailable; 11 assertion/auth failures appeared in the shared full run |
| Six failed non-Docker suites rerun with one fresh fork per class | FAIL | 14 | 1 | 0 | Auth/governance/voucher failures disappeared; only the pre-existing secondhand expected-count `1` vs `2` assertion remains |
| `npm --prefix frontend run build` | PASS | N/A | 0 | 0 | Production frontend bundle built; only existing bundle-size warnings |
| Domain-E `@UC24|@UC25`, one worker | PASS | 2 | 0 | 0 | Real backend + Messaging + MySQL + WebSocket; UC25 used order create/pay/ship → producer Outbox → Messaging notification |
| `git diff --check` | PASS | N/A | 0 | 0 | Final source/document pass |

The raw Playwright JSON, JUnit XML, HTML report, traces, screenshots, and videos are under `v2-e2e-final/`. No deployment evidence is claimed in V2.

Failure-isolation scope:

- Scenario A is exercised by UC25 against live backend, Messaging, MySQL, and WebSocket.
- Scenario B is exercised with the real order/payment transaction and relay pointed at an unavailable Messaging endpoint; payment remains committed and producer events enter `RETRY`.
- Scenario C components are verified independently (durable producer retry plus actual Messaging Inbox processing), but no single automated test restarts Messaging and follows the exact same business event through backlog recovery. This remains partial evidence.
- Scenario D replays the same processed event twice and verifies notification count remains one and audit tasks are durable.
