# UC03 test report

The H2-backed Spring integration gate passed on 2026-08-27: 2 tests, 0
failures, 0 errors. It covered submit/query, approve role and shop upgrade,
notification/audit persistence, repeat approve without duplicate core shop or
notification, rejection reason/role preservation, and injected notification
storage failure without rollback of the approval core state.

The Compose/MySQL Playwright gate is implemented but `NOT_RUN` until Docker
produces its raw report and screenshots. H2 is not represented as MySQL.
