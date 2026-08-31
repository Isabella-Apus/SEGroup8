# Final acceptance fixed-checklist matrix (0–13)

Revision: `0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`

| Requirement | Command | Actual result | Evidence | Status |
|---|---|---|---|---|
| 0. Metadata and required documentation | `Get-ChildItem 02_docs/microservices/messaging-service` | All required files present; content reviewed against current routes/config | `final-acceptance-metadata.md` | PASS |
| 1. Source boundary gate | `node scripts/ci/verify-messaging-boundary.mjs` | 39 Java files scanned; no forbidden foreign access | `source-boundary-gate.md` | PASS |
| 2. Independent Maven build | `mvn -B --no-transfer-progress -f microservices/pom.xml -pl messaging-service -am clean verify` | BUILD SUCCESS; Messaging 22 tests, 0 failures, 1 conditional MySQL skip in default reactor | Maven console / `test-plan.md` | PASS |
| 3. Runtime route contract | `node scripts/ci/verify-messaging-routes.mjs` + candidate `/v3/api-docs` | 12 normalized operations matched | `public-api-route-manifest.json`, `runtime-openapi.json` | PASS |
| 4. Public API coverage | `node scripts/ci/run-messaging-public-api-audit.mjs` | 12/12 operations successful; expected 401/403/400/404 negatives observed | `public-api-coverage-local.md` | PASS |
| 5. Idempotency contract | Messaging reliable integration tests and internal API audit | eventId/dedupeKey/idempotency records prevent duplicate notification | `v2-test-evidence.md` | PASS |
| 6. Internal authentication | Internal API audit with missing/wrong/valid credentials | missing/wrong credentials 401; valid service/operations credentials accepted | `public-api-coverage-local.md` | PASS |
| 7. Error contract and actuator isolation | Candidate hidden endpoint probes | stable JSON 404 for hidden actuator paths; no SQL/secret in response | `actuator-security-local.md` | PASS |
| 8. Real MySQL migration and permissions | `MySqlMigrationTest` against `mysql:8.4.6` | Flyway V1/V2 from empty DB, repeat deterministic, foreign-schema access denied | migration console / `migration-readiness-local.md` | PASS |
| 9. Readiness fault behavior | stop/start isolated MySQL; probe liveness/readiness | liveness 200; readiness 503 while DB stopped; readiness 200 after restart | `migration-readiness-local.md` | PASS |
| 10. Actuator endpoints/security | candidate probes for health/info/metrics/prometheus/hidden endpoints | approved endpoints 200; flyway/env/configprops/beans/heapdump 404 | `actuator-security-local.md` | PASS |
| 11. Info provenance | `GET /actuator/info` | commit equals audited full SHA; version and buildTime present | `actuator-security-local.md` | PASS |
| 12. Structured logging/redaction | Docker candidate logs and HTTP completion requests | one-line JSON fields and completion MDC observed; secrets/body/query token absent | `logging-local.md` | PASS |
| 13. End-to-end/deployment gates | candidate WS/API; K8s/registry/production smoke | local independent API/WS PASS; full-system UC24/UC25 candidate run and K8s/registry not available | `independent-service-e2e.md`, deployment/fault reports | NOT_RUN |
