# UC01 测试报告

结论：UC01 后端/API/H2 集成、Playwright spec、报告、追溯和真实 Compose + MySQL
Chromium 浏览器验收均已完成。

The H2-backed Spring integration gate passed locally on 2026-08-27:

- `IdentityUc01IntegrationTest`: 2 tests, 0 failures, 0 errors.
- Saved API coverage: `AuthControllerWebMvcTest` 4 tests and
  `AuthServiceImplTest` 5 tests, all passed.
- Domain-A baseline after A0: 33 tests, 0 failures, 0 errors.
- The test asserts persisted BCrypt, JWT uid, USER/ADMIN boundary, ban state,
  failed banned login, duplicate registration and no dirty duplicate row.

The real Compose + MySQL + Chromium gate also passed locally on 2026-08-27:

- `e2e/domain-a/uc01-auth.spec.ts`: 1 test passed, 0 failures.
- Evidence was written to `04_tests/UC01/evidence/`, including Playwright
  HTML/JSON output and Compose service logs.

H2 evidence remains separately reported; this run is the real MySQL/browser
evidence for UC01.

## 完成项

- 已完成注册、密码哈希、JWT uid/role、USER/ADMIN 边界、封禁登录失败、重复注册、
  非法参数和错误密码无脏数据的真实 H2 集成链。
- 已完成 save-epicA-changes 中 Auth Controller/Service API 覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc01-auth.spec.ts`：注册/登录主链、权限边界、
  封禁后登录失败、页面刷新回读。
- 已完成 `02_docs/UC01/traceability.md`、本报告和 `result-summary.json` 更新。
- 已完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots 目录（本次通过无失败截图）。


## 当前 CI 证据

main@b622e6bb 的 Identity Governance 流水线 33526387419 和完整系统流水线 33526387696 已复验相关 API、真实数据库、Compose/Playwright 与生产部署。完整 HTML、trace、video 和流水线日志由 Actions artifact 保存。
