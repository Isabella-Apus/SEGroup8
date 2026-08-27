# UC04 封禁解禁治理测试计划

状态：后端/API/H2 集成、测试脚本、报告与追溯已完成；真实 Compose + MySQL
浏览器执行未完成（本机 Docker Linux daemon 不可用）。

## Prompt 验收项

| 验收项 | 状态 | 主要证据 |
|---|---|---|
| 管理员封禁后用户登录失败 | 已完成 | `UserGovernanceUc04IntegrationTest` |
| 解禁后用户重新登录成功 | 已完成 | UC04 集成测试、Playwright 回归链 |
| 非管理员、自封禁被拒绝 | 已完成 | 集成测试、E2E API 边界 |
| 封禁/解禁均有可查询审计 | 已完成 | 集成测试审计查询 |
| 重复状态操作具有明确幂等语义 | 已完成 | 重复解禁集成断言、E2E 断言 |
| 主成功链、权限/异常链、刷新后回读的 E2E 脚本 | 已完成 | `frontend/e2e/domain-a/uc04-ban-unban.spec.ts` |
| 真实 Compose 前端/后端/MySQL 浏览器执行 | 未完成 | Docker 恢复后运行并回填 Evidence |

```bash
mvn -B -f backend/pom.xml -Dtest=UserGovernanceUc04IntegrationTest test
mvn -B -f backend/pom.xml "-Dtest=AdminUserControllerWebMvcTest,AdminUserServiceImplTest" test
mvn -B -f backend/pom.xml "-Dgroups=DOMAIN_A" test
mvn -B -f backend/pom.xml clean verify
cd frontend && npm ci && npm run build:real
docker compose -f compose.yml -f compose.e2e.yml config --quiet
pwsh -File scripts/e2e/run-compose-e2e.ps1
```

实际结果：Integration 1 test PASS；保存分支新增 API 覆盖 4 tests PASS，当前
Controller/Service 定向回归共 8 tests PASS；Domain-A 定向 65 tests PASS；后端
全量 127 tests PASS；frontend `npm ci` 安装 96 个包、`npm run build:real` 构建
2421 modules 均 PASS；Compose 配置检查 PASS；最后一条命令因 Docker Linux daemon
不可用为 NOT_RUN。浏览器命令应设置 `E2E_OUTPUT_DIR=04_tests/UC04/evidence`，
实际运行后才可标记 E2E 已完成。

## CI

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Evidence

- `04_tests/UC04/evidence/result-summary.json`
- `04_tests/UC04/evidence/raw-reports/`
- `04_tests/UC04/evidence/logs/`
- `04_tests/UC04/evidence/screenshots/`（Compose 未运行前暂无浏览器截图）

## 已知风险

- 当前 PASS 是 H2/MockMvc 和 Spring 集成证据，不等同于 MySQL/真实浏览器 PASS。
- 旧 Token 在封禁后的资源访问策略仍依赖项目统一的账号状态校验，需后续跨服务回归。
- Compose E2E 依赖 Docker daemon、数据库初始化和管理员/用户种子账号。
