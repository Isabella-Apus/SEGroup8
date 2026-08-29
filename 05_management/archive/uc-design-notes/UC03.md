# UC03 商家申请、审核与角色升级

状态：后端/API/H2 集成与真实 Compose + MySQL + Chromium 浏览器执行均已完成。

- 需求：`REQ03 / UC03`
- 集成测试：`MerchantApplicationUc03IntegrationTest`、
  `MerchantApplicationNotificationFailureIntegrationTest`
- 浏览器测试：`frontend/e2e/domain-a/uc03-merchant-application.spec.ts`
- 证据：`04_tests/UC03/evidence/`

通过审核必须同时更新申请、用户角色和店铺，并留下管理员审计；驳回只
更新申请状态与原因。通知是 best-effort side effect，故障不回滚核心审核结果。
