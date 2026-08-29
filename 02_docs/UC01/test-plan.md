# UC01 测试计划

状态：后端/API/H2 集成、测试脚本、报告、追溯与真实 Compose + MySQL + Chromium
浏览器执行均已完成。

## Scope

## 2026-08-27 real E2E refresh

真实 Compose + MySQL + Chromium 已执行并通过：UC01 spec 1 test passed。运行产物已写入
`04_tests/UC01/evidence/`，包括 Playwright HTML/JSON、Compose 服务日志和运行结果。

Register, login, password hashing, JWT claims, role authorization, ban/login
linkage, duplicate registration, invalid parameters and wrong passwords.

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC01-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/AuthServiceImplTest.java#register_shouldEncodePasswordAndInsertUser`; `#login_shouldUpgradeLegacyPasswordAndReturnToken`; `#login_shouldThrowWhenPasswordInvalid`; `#register_shouldRejectDuplicateUsername`; `#login_shouldRejectBannedUser` | BCrypt 不保存明文；登录返回可解析 JWT；错误密码、重复用户名和封禁用户被拒绝。 |
| `MVC-TC01-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/AuthControllerWebMvcTest.java#register_shouldReturnUnifiedSuccess`; `#register_shouldRejectInvalidBody`; `#login_shouldReturnTokenAndRole`; `#login_shouldRejectInvalidBody` | 统一响应、参数校验、Token/角色字段和非法请求状态正确。 |
| `INT-TC01-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/IdentityUc01IntegrationTest.java#registerLoginRoleBoundaryAndBanMustShareOnePersistedChain`; `#duplicateInvalidAndWrongPasswordRequestsMustNotCreateDirtyUsers` | 注册、登录、角色越权、封禁/解禁及异常请求在同一持久化链路中保持一致且无脏用户。 |
| `E2E-TC01-001` | Browser E2E | `frontend/e2e/domain-a/uc01-auth.spec.ts#register, login, role boundary, ban and refresh persistence` | 真实页面完成注册、登录、角色边界、封禁和刷新后身份持久化。 |

## Prompt 验收项

| 验收项 | 状态 | 主要证据 |
|---|---|---|
| 注册成功、密码 BCrypt 持久化且默认 USER/NORMAL | 已完成 | `IdentityUc01IntegrationTest` |
| 登录成功并返回可验证 JWT | 已完成 | `IdentityUc01IntegrationTest`、`JwtUtilsTest` |
| USER 访问 ADMIN 接口被拒绝 | 已完成 | `IdentityUc01IntegrationTest`、`uc01-auth.spec.ts` |
| 封禁用户不能登录 | 已完成 | `IdentityUc01IntegrationTest`、`uc01-auth.spec.ts` |
| 重复用户名、错误密码、非法参数无脏数据 | 已完成 | `AuthControllerWebMvcTest`、`AuthServiceImplTest`、集成测试 |
| 主成功链、越权链、刷新后回读的 E2E 脚本 | 已完成 | `frontend/e2e/domain-a/uc01-auth.spec.ts` |
| 真实 Compose 前端/后端/MySQL 浏览器执行 | 已完成 | `04_tests/UC01/evidence/playwright-report/`、`playwright-results.json` |

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
`npm run build:real` 构建 2421 modules 均 PASS；Compose 配置检查 PASS；真实浏览器
命令执行 1 test，`1 passed (3.0s)`，失败数 0；Compose 项目已由 runner 自动清理。

## 最新执行记录（2026-08-27）

- 真实命令：`$env:COMPOSE_FILE='compose.yml;compose.e2e.yml'; $env:E2E_OUTPUT_DIR='04_tests/UC01/evidence'; .\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase e2e/domain-a/uc01-auth.spec.ts`
- 实际结果：MySQL、backend、frontend 健康检查均 PASS；Chromium 执行 1 test，`1 passed (3.0s)`；Compose 项目已自动清理。
- 结论：真实 Compose + MySQL + Chromium 浏览器验收已完成；报告位于 `04_tests/UC01/evidence/playwright-report/`，结果位于 `04_tests/UC01/evidence/playwright-results.json`。

## CI

`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Evidence

- `04_tests/UC01/evidence/result-summary.json`
- `04_tests/UC01/evidence/raw-reports/`
- `04_tests/UC01/evidence/logs/`
- `04_tests/UC01/evidence/screenshots/`（本次通过无失败截图）

## 已知风险

- H2/MockMvc 与真实 MySQL/Chromium 证据分别保留，不能互相替代；本次两层均已 PASS。
- Compose E2E 依赖 Docker daemon、数据库初始化和 `user/user123` 种子账号。
- JWT 共享契约已放在 PLATFORM/global 层；本 UC 不重复计算该平台测试。
