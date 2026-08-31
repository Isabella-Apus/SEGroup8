# Actuator security/runtime evidence

Candidate endpoint: `http://127.0.0.1:18084` (Docker candidate image, isolated MySQL 8.4).

| Endpoint | HTTP | Observed result |
|---|---:|---|
| `/actuator` | 200 | Root links expose health/info/metrics/prometheus only; no flyway/env/configprops/beans/heapdump |
| `/actuator/health/liveness` | 200 | `{"status":"UP"}` |
| `/actuator/health/readiness` | 200 | `{"status":"UP"}` |
| `/actuator/info` | 200 | version `v3-final-candidate`, commit `0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`, buildTime `2026-08-31T00:00:00Z` |
| `/actuator/metrics` | 200 | Includes messaging backlog, consume failures, retry count, active WebSocket and push failures meters |
| `/actuator/prometheus` | 200 | Prometheus exposition returned |
| `/actuator/flyway` | 404 | Hidden endpoint does not leak migration details |
| `/actuator/env` | 404 | Not exposed |
| `/actuator/configprops` | 404 | Not exposed |
| `/actuator/beans` | 404 | Not exposed |
| `/actuator/heapdump` | 404 | Not exposed |

All checks were made against the running candidate image; no credentials were included in the captured response.
