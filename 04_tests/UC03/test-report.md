# UC03 测试报告

结论：UC03 后端/API/H2 集成、Playwright spec、报告和追溯已完成；真实
Compose + MySQL + 浏览器验收未完成，不能提前关闭 UC03 Task Issue。

The H2-backed Spring integration gate passed on 2026-08-27: 2 tests, 0
failures, 0 errors. It covered submit/query, approve role and shop upgrade,
notification/audit persistence, repeat approve without duplicate core shop or
notification, rejection reason/role preservation, and injected notification
storage failure without rollback of the approval core state.

Saved API coverage passed with 9 tests: `MerchantApplicationControllerWebMvcTest`
6 and `MerchantApplicationServiceImplTest` 3.

The Compose/MySQL Playwright gate is implemented but `NOT_RUN` until Docker
produces its raw report and screenshots. H2 is not represented as MySQL.

## 完成项

- 已完成提交/查询、管理员审核权限、通过后的角色/店铺/审计一致性、拒绝原因和
  角色保持、重复申请/审核副作用以及通知失败隔离的 H2 集成链。
- 已完成 save-epicA-changes 中 Merchant Application Controller/Service API
  覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc03-merchant-application.spec.ts`：提交/查询、
  管理员通过升级、拒绝原因持久化、角色未升级和页面刷新回读。
- 已完成 `02_docs/UC03/traceability.md`、本报告和 `result-summary.json` 更新。
- 未完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots。

## PR 所需信息

- CI：`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`
- 修改文件：`backend/src/test/java/com/segroup8/platform/controller/MerchantApplicationControllerWebMvcTest.java`、
  `backend/src/test/java/com/segroup8/platform/service/impl/MerchantApplicationServiceImplTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/MerchantApplicationUc03IntegrationTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/MerchantApplicationNotificationFailureIntegrationTest.java`、
  `frontend/e2e/domain-a/uc03-merchant-application.spec.ts`、`04_tests/UC03/test-plan.md`、
  `04_tests/UC03/test-report.md`、`04_tests/UC03/evidence/result-summary.json`、
  `02_docs/UC03/traceability.md`。
- Evidence：`04_tests/UC03/evidence/`；`raw-reports/` 已提交本次 11 个测试的
  Surefire XML/TXT，`logs/` 和 `screenshots/` 暂无 Compose 运行产物，未伪造浏览器证据。
- 风险：H2/MockMvc 不能证明 MySQL/Compose 行为；CI 需在 GitHub runner 上实际跑出
  E2E 结果后才能将 `Refs` 改为 `Closes`。
