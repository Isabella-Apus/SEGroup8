# UC13 evidence

后端 Evidence 来自 Testcontainers MySQL 8.4.6 定向 `clean verify`；浏览器 Evidence 来自专用 Compose MySQL。`result-summary.json` 汇总后端定向结果，Playwright 结果见 `raw-reports/playwright/playwright-results.json`。

- `logs/backend-domain-c.log`: Domain-C backend launcher output.
- `raw-reports/surefire/`: XML and text reports for the UC13 integration suite.
- `raw-reports/playwright/`: browser JSON/XML/HTML reports.
- `screenshots/`: captured browser state after fulfillment and refresh.
- `result-summary.json`: structured backend totals and exit status.
