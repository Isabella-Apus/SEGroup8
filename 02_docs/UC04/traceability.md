# UC04 traceability

状态：后端集成与 API 证据已完成；浏览器 spec 已完成但真实 Compose 执行未完成。

| Requirement / acceptance | Implementation | Integration | E2E | 状态 |
|---|---|---|---|---|
| REQ04 admin ban/unban state | `AdminUserServiceImpl` | `UserGovernanceUc04IntegrationTest` | ban/unban API-assisted browser flow | 后端已完成；浏览器待运行 |
| REQ04 banned login failure and recovery | `AuthServiceImpl.login` | same test | login before/after recovery | 后端已完成；浏览器待运行 |
| REQ04 non-admin and self-ban boundaries | `assertAdmin`, self check | same test | API permission path | 后端已完成；浏览器待运行 |
| REQ04 audit linkage/query | controller audit record + `AdminAuditLogService` | audit count/query | admin audit API query | 后端已完成；浏览器待运行 |
