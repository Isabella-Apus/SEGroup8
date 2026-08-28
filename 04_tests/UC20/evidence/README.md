# UC20 Evidence

- `screenshots/`：卖家发货、买家查看物流、买家待评价、卖家结算完成四张真实 Compose 页面截图。
- `raw-reports/playwright/`：Playwright HTML、JSON、JUnit 和测试附件；失败时包含 trace、video 与自动截图。
- `raw-reports/surefire-uc20-*.xml`：Controller、Integration、兼容流程与 Service 原始 XML。
- `logs/`：Compose 构建/启动/停止、数据库、后端、前端和 Playwright 日志。
- `result-summary.json`：本次可机器读取的汇总。

证据必须来自本机 Docker Compose 真栈，不使用 Vite mock 或人工伪造结果。日志提交前需检查，不得包含个人 token、密码、私钥或生产 Secret。
