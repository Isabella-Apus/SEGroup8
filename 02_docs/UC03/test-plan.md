# UC03 商家申请与审核测试计划

状态：后端/API/H2 集成、测试脚本、报告、追溯与真实 Compose + MySQL + Chromium
浏览器执行均已完成。

## Prompt 验收项

| 验收项 | 状态 | 主要证据 |
|---|---|---|
| 用户提交并查询申请 | 已完成 | `MerchantApplicationUc03IntegrationTest` |
| 管理员通过后角色升级、店铺创建、审计生成 | 已完成 | UC03 集成测试持久化断言 |
| 管理员拒绝后保存原因且角色不变 | 已完成 | UC03 集成测试、Playwright 拒绝链 |
| 重复申请、重复审核无重复副作用 | 已完成 | UC03 集成测试、Service API 测试 |
| 通知失败不回滚核心审核结果 | 已完成 | `MerchantApplicationNotificationFailureIntegrationTest` |
| 主成功链、权限/异常链、刷新后回读的 E2E 脚本 | 已完成 | `frontend/e2e/domain-a/uc03-merchant-application.spec.ts` |
| 真实 Compose 前端/后端/MySQL 浏览器执行 | 已完成 | `04_tests/UC03/evidence/playwright-report/`、`playwright-results.json` |

```bash
mvn -B -f backend/pom.xml "-Dtest=MerchantApplicationUc03IntegrationTest,MerchantApplicationNotificationFailureIntegrationTest" test
mvn -B -f backend/pom.xml "-Dtest=MerchantApplicationControllerWebMvcTest,MerchantApplicationServiceImplTest" test
mvn -B -f backend/pom.xml "-Dgroups=DOMAIN_A" test
mvn -B -f backend/pom.xml clean verify
cd frontend && npm ci && npm run build:real
docker compose -f compose.yml -f compose.e2e.yml config --quiet
pwsh -File scripts/e2e/run-compose-e2e.ps1
```

实际结果：UC03 两个集成类共 2 tests PASS；保存的 Controller/Service API 覆盖
9 tests PASS；Domain-A 定向 65 tests PASS；后端全量 127 tests PASS；frontend
`npm ci` 安装 96 个包、`npm run build:real` 构建 2421 modules 均 PASS；Compose
配置检查 PASS；真实浏览器命令执行 1 test，`1 passed (3.1s)`，失败数 0；Compose
项目已由 runner 自动清理。

## 最新执行记录（2026-08-27）

- 真实命令：`$env:COMPOSE_FILE='compose.yml;compose.e2e.yml'; $env:E2E_OUTPUT_DIR='04_tests/UC03/evidence'; .\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase e2e/domain-a/uc03-merchant-application.spec.ts`
- 实际结果：MySQL、backend、frontend 健康检查均 PASS；Chromium 执行 1 test，`1 passed (3.1s)`；Compose 项目已自动清理。
- 结论：真实 Compose + MySQL + Chromium 浏览器验收已完成；报告位于 `04_tests/UC03/evidence/playwright-report/`，结果位于 `04_tests/UC03/evidence/playwright-results.json`。

## CI

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Evidence

- `04_tests/UC03/evidence/result-summary.json`
- `04_tests/UC03/evidence/raw-reports/`
- `04_tests/UC03/evidence/logs/`
- `04_tests/UC03/evidence/screenshots/`（本次通过无失败截图）

## 已知风险

- H2/MockMvc 与真实 MySQL/Chromium 证据分别保留，不能互相替代；本次两层均已 PASS。
- 通知失败隔离只在注入故障的后端集成测试中验证，真实外部通知服务仍需后续回归。
- Compose E2E 依赖 Docker daemon、数据库初始化和管理员种子账号。
