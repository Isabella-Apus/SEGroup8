# UC18 Evidence

- `screenshots/`：买家待处理、卖家确认、买家待付款三张真实 Compose 页面截图。
- `raw-reports/playwright/`：Playwright HTML、JSON、JUnit 和附件。
- `raw-reports/surefire-*.xml`：Integration 与回归测试原始 XML。
- `logs/`：Compose 构建/启动/停止、数据库、后端、前端和 Playwright 日志。
- `result-summary.json`：本次可机器读取的汇总。

证据来自本机 Docker Compose 真栈，不是 Vite mock 或人工走查；提交前的后端全量 `mvn clean verify` 共 110 项测试全部通过。
