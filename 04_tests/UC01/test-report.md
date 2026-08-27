# UC01 test report

The H2-backed Spring integration gate passed locally on 2026-08-27:

- `IdentityUc01IntegrationTest`: 2 tests, 0 failures, 0 errors.
- Domain-A baseline after A0: 33 tests, 0 failures, 0 errors.
- The test asserts persisted BCrypt, JWT uid, USER/ADMIN boundary, ban state,
  failed banned login, duplicate registration and no dirty duplicate row.

The Compose/MySQL browser gate is implemented but must be recorded as
`NOT_RUN` until the Docker runner is executed. H2 evidence is not represented
as MySQL or browser evidence.
