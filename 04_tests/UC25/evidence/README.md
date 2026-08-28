# UC25 Evidence

- `raw-reports/TEST-*.xml`：UC25 与 Domain E 后端 Surefire 结果。
- `raw-reports/playwright/`：真实 Edge 的 Playwright JSON、XML、HTML、trace 和失败媒体。
- `screenshots/uc25-realtime-reconnect-and-read.png`：重连补拉并已读持久化后的页面。
- `logs/runtime-health.txt`：Compose 与 HTTP 健康检查。
- `result-summary.json`：机器可读汇总，结构遵循 Domain E schema。

本目录不保存密码、JWT、私钥或生产 Secret。
