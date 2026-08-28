# UC11 测试报告

## 执行状态

报告由本分支实际验证结果更新。不得以测试计划、Mock 声明或其他 UC 测试替代 UC11 结果。

| 验证项 | 命令/入口 | 当前结果 | 原始证据 |
|---|---|---|---|
| UC11 Domain-C 快速回归 | `run-domain-c-tests.mjs --suite UC11 --goal verify` | PASS：7/7，失败 0，错误 0，跳过 0 | `evidence/result-summary.json`、`evidence/raw-reports/surefire/`、`evidence/logs/backend-domain-c.log` |
| UC11 MySQL Integration | `OrderCreateUc11IntegrationTest` + MySQL 8.4.6 | PASS：7/7，失败 0，错误 0，跳过 0 | `evidence/raw-reports/mysql/`、`evidence/logs/backend-mysql-integration.log` |
| UC11 浏览器 E2E | `uc11-checkout-order.spec.ts --workers=1` | PASS：1/1，unexpected 0，flaky 0，skipped 0 | `evidence/raw-reports/playwright/`、`evidence/screenshots/uc11-checkout-order-persisted.png`、`evidence/logs/playwright.log` |
| 后端全量回归 | `mvn clean verify` | PASS：99/99，失败 0，错误 0，跳过 0 | `evidence/logs/backend-full-verify.log`；CI 合并前复核 |
| 前端真实构建 | `npm run build:real` | PASS：Vite real-mode 构建完成 | 本次本地构建输出；CI 合并前复核 |
| Evidence 校验 | `verify-evidence.mjs`、`verify-playwright-evidence.mjs` | PASS：1 份 Surefire XML、1 个 E2E、1 张成功截图 | 上述 Evidence 目录 |

## 场景结论

真实 MySQL 8.4.6 Integration 已覆盖创建主链、重复项合并与服务端计价、商品和交易资格拒绝、优惠券边界、券占用后的后续明细写入失败事务回滚、幂等回放及 HTTP/数据库一致性。浏览器 E2E 已在 MySQL 8.4.6、真实后端 JAR 和 real-mode 前端上完成购物车到订单详情链路，并在刷新后重新查询确认持久化。

本地统一 Compose 入口在 `compose-build` 阶段两次因 Docker 镜像代理 `image-mirror.r2.daocloud.vip` 返回 `EOF` 而非零退出，未进入应用启动；失败阶段和构建日志保存在 `evidence/logs/`。为验证业务链路，改用同一 `compose.yml` 初始化的 MySQL、同一后端产物和同一 Playwright spec 完成上述 PASS。PR 合并前仍须由 CI 的独立 Compose E2E Job 复核。

最终结果以 `evidence/result-summary.json`、Surefire XML 和 Playwright JSON 中一致的计数为准。CI 与非作者 Review 未完成前，PR 正文使用 `Refs #UC11-TaskIssue`，不得关闭 UC11 父 Issue #45。
