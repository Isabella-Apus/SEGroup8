# UC04 封禁、解禁、登录联动与审计

状态：后端/API/H2 集成与真实 Compose + MySQL + Chromium 浏览器执行均已完成。

- 需求：`REQ04 / UC04`
- 集成测试：`UserGovernanceUc04IntegrationTest`
- 浏览器测试：`frontend/e2e/domain-a/uc04-ban-unban.spec.ts`
- 证据：`04_tests/UC04/evidence/`

测试覆盖封禁后登录失败、解禁后恢复、普通用户越权、管理员自保护、重复
解禁幂等结果，以及审计日志查询权限和记录数量。
