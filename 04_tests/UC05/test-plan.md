# UC05 举报、拉黑与信用治理测试计划

状态：后端/API/H2 集成、测试脚本、报告、追溯与真实 Compose + MySQL + Chromium
浏览器执行均已完成。

## 分层

- MockMvc：验证举报、拉黑、信用和管理员接口的响应契约、参数校验和权限边界。
- H2 集成：从注册用户开始，验证真实 mapper、事务状态和跨表一致性。
- Compose E2E：连接 Docker Compose 的 frontend/backend/MySQL，验证页面刷新后举报记录与信用分仍然存在。

## Prompt 验收项

| 验收项 | 状态 | 主要证据 |
|---|---|---|
| 举报提交、本人查询 | 已完成 | `ReportBlockCreditUc05IntegrationTest` |
| 拉黑、查询、解除及双方数据隔离 | 已完成 | UC05 集成测试、Playwright API 断言 |
| 管理员审核举报 | 已完成 | UC05 集成测试、`uc05-governance.spec.ts` |
| 信用分、`credit_score_log`、`admin_audit_log` 一致 | 已完成 | UC05 集成测试事务断言 |
| 重复审核、自己举报/拉黑、非管理员拒绝 | 已完成 | Controller/Service API 与集成测试 |
| 主成功链、权限/异常链、刷新后回读的 E2E 脚本 | 已完成 | `frontend/e2e/domain-a/uc05-governance.spec.ts` |
| 真实 Compose 前端/后端/MySQL 浏览器执行 | 已完成 | `04_tests/UC05/evidence/playwright-report/`、`playwright-results.json` |

## 命令

```powershell
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
mvn -B -f backend/pom.xml "-Dtest=ReportBlockCreditUc05IntegrationTest,ReportBlockControllerWebMvcTest,ReportBlockServiceImplTest" test
mvn -B -f backend/pom.xml clean verify
cd frontend
npm ci
npm run build:real
npm.cmd run e2e -- e2e/domain-a/uc05-governance.spec.ts --list
docker compose -f compose.yml -f compose.e2e.yml config --quiet
pwsh -File scripts/e2e/run-compose-e2e.ps1
```

实际结果：`ReportBlockCreditUc05IntegrationTest` 1 test PASS；Controller 11
tests、Service 6 tests，共 17 个 API/MockMvc 测试 PASS；Domain-A 定向 65 tests
PASS；后端全量 127 tests PASS；frontend `npm ci` 安装 96 个包、`npm run
build:real` 构建 2421 modules 均 PASS；Compose 配置检查 PASS；真实浏览器命令
执行 1 test，`1 passed (3.2s)`，失败数 0；Compose 项目已由 runner 自动清理。

## 最新执行记录（2026-08-27）

- 真实命令：`$env:COMPOSE_FILE='compose.yml;compose.e2e.yml'; $env:E2E_OUTPUT_DIR='04_tests/UC05/evidence'; .\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase e2e/domain-a/uc05-governance.spec.ts`
- 实际结果：MySQL、backend、frontend 健康检查均 PASS；Chromium 执行 1 test，`1 passed (3.2s)`；Compose 项目已自动清理。
- 结论：真实 Compose + MySQL + Chromium 浏览器验收已完成；报告位于 `04_tests/UC05/evidence/playwright-report/`，结果位于 `04_tests/UC05/evidence/playwright-results.json`。

## CI

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Evidence

- `04_tests/UC05/evidence/result-summary.json`
- `04_tests/UC05/evidence/raw-reports/`
- `04_tests/UC05/evidence/logs/`
- `04_tests/UC05/evidence/screenshots/`（本次通过无失败截图）

## 已知风险

- 当前 PASS 是 H2/MockMvc 和 Spring 集成证据，不等同于 MySQL/真实浏览器 PASS。
- 信用扣分与审计一致性验证的是当前单体事务；跨服务迁移后的最终一致性仍需回归。
- Compose E2E 依赖 Docker daemon、数据库初始化和管理员/用户种子账号。
