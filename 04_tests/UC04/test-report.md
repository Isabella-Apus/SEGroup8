# UC04 测试报告

结论：UC04 后端/API/H2 集成、Playwright spec、报告、追溯和真实 Compose + MySQL
Chromium 浏览器验收均已完成。

The H2-backed Spring integration gate passed on 2026-08-27: 1 test, 0
failures, 0 errors. It verified the persisted BANNED/NORMAL transitions,
failed banned login, successful recovery, non-admin/self-ban boundaries,
repeat unban state, and audit query permission.

Saved API coverage passed with 2 added tests in `AdminUserControllerWebMvcTest`
and 2 added tests in `AdminUserServiceImplTest` (4 tests total).

The real Compose/MySQL/Chromium Playwright gate passed on 2026-08-27: 1 test,
0 failures, 0 errors. It started the real database/backend/frontend stack,
performed health checks, executed `uc04-ban-unban.spec.ts`, and cleaned the
Compose project afterward. H2 evidence remains reported separately.

## 完成项

- 已完成封禁→登录失败、解禁→登录成功、非管理员/自封禁拒绝、审计查询和重复解禁
  幂等的真实 H2 集成链。
- 已完成 save-epicA-changes 中 Admin User Controller/Service API 覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc04-ban-unban.spec.ts`：主成功链、登录恢复、
  重复解禁、非管理员/自封禁边界、审计查询和页面刷新。
- 已完成 `02_docs/UC04/traceability.md`、本报告和 `result-summary.json` 更新。
- 已完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots 目录（本次通过无失败截图）。

## PR 所需信息

- CI：`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`
- 修改文件：`backend/src/test/java/com/segroup8/platform/controller/AdminUserControllerWebMvcTest.java`、
  `backend/src/test/java/com/segroup8/platform/service/impl/AdminUserServiceImplTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/UserGovernanceUc04IntegrationTest.java`、
  `frontend/e2e/domain-a/uc04-ban-unban.spec.ts`、`04_tests/UC04/test-plan.md`、
  `04_tests/UC04/test-report.md`、`04_tests/UC04/evidence/result-summary.json`、
  `02_docs/UC04/traceability.md`。
- Evidence：`04_tests/UC04/evidence/`；`raw-reports/` 已提交本次 9 个后端测试的
  Surefire XML/TXT，`logs/`、`playwright-report/`、`playwright-results.json` 和
  `test-results/.last-run.json` 已提交真实 Compose 浏览器运行产物。
- 风险：本地 Docker 使用了缓存镜像别名以绕过 Docker Hub 证书问题；CI 仍需能够拉取
  workflow 中声明的上游镜像。旧 Token 在封禁后的资源访问策略仍需跨服务回归。
