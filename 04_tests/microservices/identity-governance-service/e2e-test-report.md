# Domain A E2E 报告

## 改造前单体基线

- 版本：`monolith-start` / `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119`
- 日期：2026-08-30
- 断言：复用当前同一组 `frontend/e2e/domain-a/uc01-uc05*.spec.ts`
- 结果：5/5 passed，0 failed，耗时 9.9 秒
- 原始结果：`evidence/monolith-start-playwright-20260830/`

## 改造后微服务

最终状态：`PASS`，5 passed / 0 failed / 0 skipped / 0 errors，耗时 33.0 秒。

既有真实浏览器 spec 位于：

- `frontend/e2e/domain-a/uc01-auth.spec.ts`
- `frontend/e2e/domain-a/uc02-profile-address.spec.ts`
- `frontend/e2e/domain-a/uc03-merchant-application.spec.ts`
- `frontend/e2e/domain-a/uc04-ban-unban.spec.ts`
- `frontend/e2e/domain-a/uc05-governance.spec.ts`

运行拓扑：既有 `frontend/dist` → 临时 E2E Nginx（8089）→ `identity-governance-service`（8091）→ 独立 MySQL。没有复制或改写 spec，也没有改变根网关。

首次运行：0/5，Nginx 仍代理单体端口 `backend:8080`，五条均收到 502 HTML；完整截图、视频、trace、JSON/JUnit 保留在 `evidence/domain-a-playwright/`。修正 E2E profile 把代理目标改为 `backend:8091` 后，重新运行：

| UC | 结果 | 时间 |
|---|---|---|
| UC01 注册登录/角色/封禁 | PASS | 5.8s |
| UC02 资料地址/所有权 | PASS | 5.8s |
| UC03 申请审核/角色升级/驳回 | PASS | 4.8s |
| UC04 封禁解禁/审计 | PASS | 6.2s |
| UC05 举报拉黑信用 | PASS | 4.6s |

成功原始 JSON、JUnit XML 和 HTML 报告位于 `evidence/domain-a-playwright-rerun-1/`。这证明本服务的 Domain A 浏览器回归；不代表六个目标微服务或全站 E2E 已完成。
