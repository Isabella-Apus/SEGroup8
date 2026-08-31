# Final acceptance API test report

The V2 automated suite covers internal authentication, notification idempotency,
replay, delivery status, and all public JWT APIs. V3 adds the runtime surfaces:

| Endpoint | Evidence | Status |
|---|---|---|
| `/actuator/health/liveness` | `application.yml` probe group; `evidence/raw-reports/runtime-probes-local.md` | PASS (local runtime) |
| `/actuator/health/readiness` | DB-aware readiness group; `evidence/raw-reports/runtime-probes-local.md` | PASS (local runtime) |
| `/actuator/info` | APP_VERSION/APP_COMMIT/APP_BUILD_TIME; `evidence/raw-reports/runtime-probes-local.md` | PASS (local runtime) |
| `/actuator/metrics/*` and `/actuator/prometheus` | `MessagingMetrics` meters; `evidence/raw-reports/actuator-security-local.md` | PASS (candidate runtime) |
| `/internal/*` | `InternalServiceInterceptor` | PASS (V2 integration tests) |

The reviewed runtime route set is 12/12; see `public-api-coverage-local.md`.
Production HTTP smoke is pending a reachable deployment environment and is not
claimed here.
