# UC03 测试报告

结论：UC03 后端/API/H2 集成、Playwright spec、报告、追溯和真实 Compose + MySQL
Chromium 浏览器验收均已完成。

The H2-backed Spring integration gate passed on 2026-08-27: 2 tests, 0
failures, 0 errors. It covered submit/query, approve role and shop upgrade,
notification/audit persistence, repeat approve without duplicate core shop or
notification, rejection reason/role preservation, and injected notification
storage failure without rollback of the approval core state.

Saved API coverage passed with 9 tests: `MerchantApplicationControllerWebMvcTest`
6 and `MerchantApplicationServiceImplTest` 3.

The real Compose/MySQL/Chromium Playwright gate passed on 2026-08-27: 1 test,
0 failures, 0 errors. The run started the real database/backend/frontend stack,
performed health checks, executed `uc03-merchant-application.spec.ts`, and
cleaned the Compose project afterward. H2 evidence remains reported separately.

## 完成项

- 已完成提交/查询、管理员审核权限、通过后的角色/店铺/审计一致性、拒绝原因和
  角色保持、重复申请/审核副作用以及通知失败隔离的 H2 集成链。
- 已完成 save-epicA-changes 中 Merchant Application Controller/Service API
  覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc03-merchant-application.spec.ts`：提交/查询、
  管理员通过升级、拒绝原因持久化、角色未升级和页面刷新回读。
- 已完成 `02_docs/UC03/traceability.md`、本报告和 `result-summary.json` 更新。
- 已完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots 目录（本次通过无失败截图）。

## PR 所需信息

- CI：`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`
- 修改文件：`backend/src/test/java/com/segroup8/platform/controller/MerchantApplicationControllerWebMvcTest.java`、
  `backend/src/test/java/com/segroup8/platform/service/impl/MerchantApplicationServiceImplTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/MerchantApplicationUc03IntegrationTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/MerchantApplicationNotificationFailureIntegrationTest.java`、
  `frontend/e2e/domain-a/uc03-merchant-application.spec.ts`、`04_tests/UC03/test-plan.md`、
  `04_tests/UC03/test-report.md`、`04_tests/UC03/evidence/result-summary.json`、
  `02_docs/UC03/traceability.md`。
- Evidence：`04_tests/UC03/evidence/`；`raw-reports/` 已提交本次 11 个后端测试的
  Surefire XML/TXT，`logs/`、`playwright-report/`、`playwright-results.json` 和
  `test-results/.last-run.json` 已提交真实 Compose 浏览器运行产物。
- 风险：本地 Docker 使用了缓存镜像别名以绕过 Docker Hub 证书问题；CI 仍需能够拉取
  workflow 中声明的上游镜像。通知失败隔离仍是注入故障验证，外部通知服务需后续回归。
