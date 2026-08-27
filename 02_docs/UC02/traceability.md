# UC02 traceability

状态：后端集成与 API 证据已完成；浏览器 spec 已完成但真实 Compose 执行未完成。

| Requirement / acceptance | Implementation | Integration | E2E | 状态 |
|---|---|---|---|---|
| REQ02 profile update and re-query | `UserServiceImpl.updateCurrentUserProfile` | profile assertions in `ProfileAddressUc02IntegrationTest` | profile reload | 后端已完成；浏览器待运行 |
| REQ02 address create/update/delete | `UserServiceImpl` address methods | same test | address page reload and delete refresh | 后端已完成；浏览器待运行 |
| REQ02 max one default per owner | `clearDefaultAddress` | database count assertion | response assertion | 后端已完成；浏览器待运行 |
| REQ02 cross-user isolation and unauthenticated rejection | `getOwnedAddress`, JWT interceptor | ownership test | API-assisted E2E | 已完成 |
