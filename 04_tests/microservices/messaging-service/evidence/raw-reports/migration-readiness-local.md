# Migration/readiness fault evidence

Candidate image revision: `0bafb9d6a82276f57e4fb6a66d5e50e4ce146ea5`.

- Isolated MySQL: `mysql:8.4.6`; Flyway V1 and V2 applied from an empty `messaging_db`.
- Repeated initialization: deterministic; Flyway reported schema version `2` and no pending migration.
- With MySQL container stopped: liveness returned HTTP 200/`UP`; readiness returned HTTP 503/`DOWN`.
- After MySQL restart: readiness returned HTTP 200/`UP`.
- Governance fallback is a non-critical downstream path; its unavailability does not terminate the process (communication decisions fail closed where state cannot be confirmed).
