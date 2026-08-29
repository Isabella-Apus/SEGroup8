# Domain A E2E 报告

状态：`NOT_RUN`。

既有真实浏览器 spec 位于：

- `frontend/e2e/domain-a/uc01-auth.spec.ts`
- `frontend/e2e/domain-a/uc02-profile-address.spec.ts`
- `frontend/e2e/domain-a/uc03-merchant-application.spec.ts`
- `frontend/e2e/domain-a/uc04-ban-unban.spec.ts`
- `frontend/e2e/domain-a/uc05-governance.spec.ts`

本任务没有复制 spec，也没有把根 Nginx/Compose 从单体切到新服务，因此当前 Maven/API 成功不能写成微服务版 `E2E PASS`。流量切换后应按 `@DOMAIN_A @UC01` 至 `@UC05` 分别运行并保存 Playwright JSON、trace、截图与退出码。
