# UC04 test report

The H2-backed Spring integration gate passed on 2026-08-27: 1 test, 0
failures, 0 errors. It verified the persisted BANNED/NORMAL transitions,
failed banned login, successful recovery, non-admin/self-ban boundaries,
repeat unban state, and audit query permission.

Saved API coverage passed with 2 added tests in `AdminUserControllerWebMvcTest`
and 2 added tests in `AdminUserServiceImplTest` (4 tests total).

The Compose/MySQL Playwright gate is implemented but `NOT_RUN` until Docker
produces raw reports and screenshots. H2 is not represented as MySQL.
