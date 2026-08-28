# UC24 Evidence

- `raw-reports/TEST-*.xml`：本机 UC24 后端 Surefire 结果。
- `raw-reports/playwright/playwright-results.json` 和 `playwright-results.xml`：真实 Compose + Microsoft Edge 结果。
- `raw-reports/playwright/playwright-report/index.html`：离线 HTML 报告。
- `screenshots/uc24-chat-history-and-block.png`：买卖双方持久化历史和买家拉黑后的页面状态。
- `logs/runtime-health.txt`：Nginx、Spring Boot 和 MySQL 健康检查。
- `result-summary.json`：符合 Domain E schema 的机器可读汇总。

本目录不保存密码、token、私钥或生产 Secret。Playwright 使用仓库测试账号，报告不记录登录响应中的 token。
