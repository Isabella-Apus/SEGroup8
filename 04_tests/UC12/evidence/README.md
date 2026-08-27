# UC12 evidence

后端 Evidence 来自 Testcontainers MySQL 8.4.6 定向 `clean verify`；浏览器 Evidence 来自专用 Compose MySQL。`result-summary.json` 只汇总后端定向结果，Playwright 结果以 `raw-reports/playwright/playwright-results.json` 为准。

Generated backend and browser evidence belongs here:

- `logs/backend-domain-c.log` - Domain-C launcher output.
- `raw-reports/surefire/` - Surefire XML and text reports.
- `screenshots/` - browser screenshots after refresh/requery.
- `result-summary.json` - machine-readable backend totals and result.

Generate backend evidence with:

```powershell
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs --suite UC12 --goal verify --maven-repository backend/.m2repo
```
