# UC02 用户资料与地址测试计划

状态：后端/API/H2 集成、真实 Compose + MySQL + Chromium 浏览器执行、测试脚本、
报告、追溯与 Evidence 均已完成。

## Prompt 验收项

| 验收项 | 状态 | 主要证据 |
|---|---|---|
| 修改本人资料后重新查询一致 | 已完成 | `ProfileAddressUc02IntegrationTest` |
| 地址新增、修改、删除真实落库 | 已完成 | `ProfileAddressUc02IntegrationTest` |
| 默认地址最多一个 | 已完成 | 集成测试数据库断言、Playwright API 断言 |
| 不能操作他人地址 | 已完成 | 集成测试、`uc02-profile-address.spec.ts` |
| 删除后重新查询不可见 | 已完成 | 集成测试、E2E 删除后刷新断言 |
| 主成功链、越权链、刷新后回读的 E2E 脚本 | 已完成 | `frontend/e2e/domain-a/uc02-profile-address.spec.ts` |
| 真实 Compose 前端/后端/MySQL 浏览器执行 | 已完成 | `evidence/playwright-results.json`、`playwright-report/`、`logs/` |

```bash
mvn -B -f backend/pom.xml -Dtest=ProfileAddressUc02IntegrationTest test
mvn -B -f backend/pom.xml -Dtest=UserControllerWebMvcTest test
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
mvn -B -f backend/pom.xml clean verify
cd frontend && npm ci && npm run build:real
docker compose -f compose.yml -f compose.e2e.yml config --quiet
pwsh -File scripts/e2e/run-compose-e2e.ps1
```

实际结果：Integration 2 tests PASS；保存的 `UserControllerWebMvcTest` 8 tests
PASS；Domain-A 定向 65 tests PASS；后端全量 127 tests PASS；frontend `npm ci`
安装 96 个包、`npm run build:real` 构建 2421 modules 均 PASS；Compose 配置检查
PASS；真实 Compose + MySQL + Chromium UC02 spec 1 test PASS。浏览器运行命令设置
`E2E_OUTPUT_DIR=04_tests/UC02/evidence`，结果已回填 Evidence。

## CI

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Evidence

- `04_tests/UC02/evidence/result-summary.json`
- `04_tests/UC02/evidence/raw-reports/`
- `04_tests/UC02/evidence/logs/`
- `04_tests/UC02/evidence/screenshots/`（本次通过，无失败截图）

## 已知风险

- 当前 PASS 是 H2/MockMvc 和 Spring 集成证据，不等同于 MySQL/真实浏览器 PASS。
- Compose E2E 依赖 Docker daemon、数据库初始化和可用的种子账号。
- 地址默认值和归属断言依赖后端当前状态码/字段契约，跨版本接口变化时需同步。
