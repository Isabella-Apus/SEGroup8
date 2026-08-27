# UC02 test report

The H2-backed Spring integration gate passed on 2026-08-27: 2 tests, 0
failures, 0 errors. It verified persisted profile changes, two default
address writes collapsing to one default, CRUD re-query, cross-user rejection
and unauthenticated rejection.

Saved API coverage in `UserControllerWebMvcTest` passed with 8 tests, including
the `/me`, profile update and address update contracts.

The Compose/MySQL Playwright gate is implemented and remains `NOT_RUN` until a
Docker run produces raw reports and screenshots. H2 is not reported as MySQL.
