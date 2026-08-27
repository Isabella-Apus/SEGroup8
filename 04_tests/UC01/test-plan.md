# UC01 测试计划

状态：后端/API/H2 集成、测试脚本、报告与追溯已完成；真实 Compose + MySQL
浏览器执行未完成（本机 Docker Linux daemon 不可用）。

## Scope

Register, login, password hashing, JWT claims, role authorization, ban/login
linkage, duplicate registration, invalid parameters and wrong passwords.

## Prompt 验收项

| 验收项 | 状态 | 主要证据 |
|---|---|---|
| 注册成功、密码 BCrypt 持久化且默认 USER/NORMAL | 已完成 | `IdentityUc01IntegrationTest` |
| 登录成功并返回可验证 JWT | 已完成 | `IdentityUc01IntegrationTest`、`JwtUtilsTest` |
| USER 访问 ADMIN 接口被拒绝 | 已完成 | `IdentityUc01IntegrationTest`、`uc01-auth.spec.ts` |
| 封禁用户不能登录 | 已完成 | `IdentityUc01IntegrationTest`、`uc01-auth.spec.ts` |
| 重复用户名、错误密码、非法参数无脏数据 | 已完成 | `AuthControllerWebMvcTest`、`AuthServiceImplTest`、集成测试 |
| 主成功链、越权链、刷新后回读的 E2E 脚本 | 已完成 | `frontend/e2e/domain-a/uc01-auth.spec.ts` |
| 真实 Compose 前端/后端/MySQL 浏览器执行 | 未完成 | Docker 恢复后运行并回填 Evidence |

## 本地命令与实际结果（2026-08-27）

```bash
mvn -B -f backend/pom.xml -Dtest=IdentityUc01IntegrationTest test
mvn -B -f backend/pom.xml -Dtest=AuthControllerWebMvcTest,AuthServiceImplTest test
mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test
mvn -B -f backend/pom.xml clean verify
cd frontend && npm ci && npm run build:real
docker compose -f compose.yml -f compose.e2e.yml config --quiet
pwsh -File scripts/e2e/run-compose-e2e.ps1
```

实际结果：Integration 2 tests PASS；保存的 API 覆盖 9 tests PASS；Domain-A
定向 65 tests PASS；后端全量 127 tests PASS；frontend `npm ci` 安装 96 个包、
`npm run build:real` 构建 2421 modules 均 PASS；Compose 配置检查 PASS；最后一条
命令因 Docker Linux daemon 不可用为 NOT_RUN。浏览器命令应设置
`E2E_OUTPUT_DIR=04_tests/UC01/evidence`，实际运行后才可标记 E2E 已完成。

## CI

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Evidence

- `04_tests/UC01/evidence/result-summary.json`
- `04_tests/UC01/evidence/raw-reports/`
- `04_tests/UC01/evidence/logs/`
- `04_tests/UC01/evidence/screenshots/`（Compose 未运行前暂无浏览器截图）

## 已知风险

- 当前 PASS 是 H2/MockMvc 和 Spring 集成证据，不等同于 MySQL/真实浏览器 PASS。
- Compose E2E 依赖 Docker daemon、数据库初始化和 `user/user123` 种子账号。
- JWT 共享契约已放在 PLATFORM/global 层；本 UC 不重复计算该平台测试。
