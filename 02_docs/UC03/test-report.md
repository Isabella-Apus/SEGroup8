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


## 当前 CI 证据

main@b622e6bb 的 Identity Governance 流水线 33526387419 和完整系统流水线 33526387696 已复验相关 API、真实数据库、Compose/Playwright 与生产部署。完整 HTML、trace、video 和流水线日志由 Actions artifact 保存。
