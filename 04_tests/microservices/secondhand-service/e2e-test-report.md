# MS-04 E2E 测试报告

## 两层验收

| 层次 | 被测对象 | 用例 | 最近结果 |
|---|---|---|---|
| 独立服务 API E2E | 真实 `secondhand-service` 镜像 + MySQL 8.4.6 + 订单契约桩 | UC16-UC19 | 本地 `4/4 PASS`，6.3 秒，2026-08-30 |
| 完整系统浏览器 E2E | Nginx 前端 + 完整系统后端 + MySQL，Playwright Chromium | UC16-UC20 | 已归档 `5/5 PASS` |

独立服务测试位于 `frontend/e2e/microservices/secondhand-service-api.spec.ts`，验证镜像启动、Flyway、JWT、真实数据库持久化以及二手服务到订单服务的 HTTP 契约。完整系统测试继续唯一引用 `frontend/e2e/domain-d/`，没有复制 UI 用例。

CI 的完整 HTML、trace、video、重复截图和 Compose 日志统一写入 `${{ runner.temp }}` 并上传 GitHub Actions artifact。Git 只长期保留 JSON/XML、摘要和一次关键失败截图/上下文：

- 最终 UI 机器报告：`evidence/playwright-final/playwright-results.json`、`playwright-results.xml`
- 独立服务机器报告：`evidence/playwright-independent/playwright-results.json`、`playwright-results.xml`
- UC18 修复前后证据：`evidence/playwright/`、`evidence/playwright-uc18-rerun/`
- 关键失败证据：`evidence/raw-reports/playwright-failure/uc18-initial-failure-1.png` 与上下文说明

远程流水线结果必须以本次提交对应的 Actions run 为准；本地通过不替代远程结论。
