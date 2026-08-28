# UC14 Evidence

- `result-summary.json`: UC14 tagged Maven run, exit code and aggregate counts.
- `raw-reports/surefire/`: original JUnit XML/text for the MySQL integration and WebMvc tests.
- `logs/backend-domain-c.log`: complete Maven and Testcontainers log, including the MySQL 8.4.6 startup.
- `raw-reports/playwright/`: original browser report, JSON/XML result and screenshot attachments.
- `screenshots/`: persisted after-sale page captured after administrator arbitration.

The backend evidence was regenerated on 2026-08-27 with the production `schema.sql` in a MySQL 8.4.6 Testcontainer. Historical H2 flow tests are tagged `PLATFORM` and are not counted as UC14 database evidence.
