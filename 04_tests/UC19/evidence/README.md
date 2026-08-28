# UC19 Evidence

- `screenshots/`：卖家创建、买家 B 领先、卖家查看两次出价、赢家待发货订单四张真实 Compose 页面截图。
- `raw-reports/playwright/`：Playwright HTML、JSON、JUnit 和测试附件。
- `raw-reports/surefire-uc19-*.xml`：Controller、Integration 与 Service 原始 XML。
- `logs/`：Compose 构建/启动/停止、数据库、后端、前端和 Playwright 日志。
- `result-summary.json`：本次可机器读取的汇总。

证据来自本机 Docker Compose 真栈，不是 Vite mock 或人工走查；后端全量 `mvn clean verify` 共 161 项测试全部通过。
