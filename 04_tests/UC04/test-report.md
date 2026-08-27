# UC04 测试报告

结论：UC04 后端/API/H2 集成、Playwright spec、报告和追溯已完成；真实
Compose + MySQL + 浏览器验收未完成，不能提前关闭 UC04 Task Issue。

The H2-backed Spring integration gate passed on 2026-08-27: 1 test, 0
failures, 0 errors. It verified the persisted BANNED/NORMAL transitions,
failed banned login, successful recovery, non-admin/self-ban boundaries,
repeat unban state, and audit query permission.

Saved API coverage passed with 2 added tests in `AdminUserControllerWebMvcTest`
and 2 added tests in `AdminUserServiceImplTest` (4 tests total).

The Compose/MySQL Playwright gate is implemented but `NOT_RUN` until Docker
produces raw reports and screenshots. H2 is not represented as MySQL.

## 完成项

- 已完成封禁→登录失败、解禁→登录成功、非管理员/自封禁拒绝、审计查询和重复解禁
  幂等的真实 H2 集成链。
- 已完成 save-epicA-changes 中 Admin User Controller/Service API 覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc04-ban-unban.spec.ts`：主成功链、登录恢复、
  重复解禁、非管理员/自封禁边界、审计查询和页面刷新。
- 已完成 `02_docs/UC04/traceability.md`、本报告和 `result-summary.json` 更新。
- 未完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots。

## PR 所需信息

- CI：`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`
- 修改文件：`backend/src/test/java/com/segroup8/platform/controller/AdminUserControllerWebMvcTest.java`、
  `backend/src/test/java/com/segroup8/platform/service/impl/AdminUserServiceImplTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/UserGovernanceUc04IntegrationTest.java`、
  `frontend/e2e/domain-a/uc04-ban-unban.spec.ts`、`04_tests/UC04/test-plan.md`、
  `04_tests/UC04/test-report.md`、`04_tests/UC04/evidence/result-summary.json`、
  `02_docs/UC04/traceability.md`。
- Evidence：`04_tests/UC04/evidence/`；`raw-reports/` 已提交本次 9 个测试的
  Surefire XML/TXT，`logs/` 和 `screenshots/` 暂无 Compose 运行产物，未伪造浏览器证据。
- 风险：H2/MockMvc 不能证明 MySQL/Compose 行为；CI 需在 GitHub runner 上实际跑出
  E2E 结果后才能将 `Refs` 改为 `Closes`。
