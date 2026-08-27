# UC04 test report

The H2-backed Spring integration gate passed on 2026-08-27: 1 test, 0
failures, 0 errors. It verified the persisted BANNED/NORMAL transitions,
failed banned login, successful recovery, non-admin/self-ban boundaries,
repeat unban state, and audit query permission.

The Compose/MySQL Playwright gate is implemented but `NOT_RUN` until Docker
produces raw reports and screenshots. H2 is not represented as MySQL.
