# UC17 Evidence

- `logs/`：Maven、Compose、Playwright 和服务日志。
- `raw-reports/`：Surefire XML、Playwright JSON/JUnit/HTML 等机器可读原始报告。
- `screenshots/`：真实 Compose Chromium 截图。
- `result-summary.json`：测试命令、退出码、数量和环境摘要。

截图只在真实 Nginx 前端经真实 API 访问 MySQL 时标记为 E2E Evidence。
