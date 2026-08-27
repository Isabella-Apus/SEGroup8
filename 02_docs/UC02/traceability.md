# UC02 traceability

2026-08-27 refresh: 后端集成与真实 Compose + MySQL + Chromium Playwright
执行均已完成；本次 UC02 spec 通过 1 个测试。对应 raw report、HTML/JSON
结果和 Compose 日志位于 `04_tests/UC02/evidence/`。

状态：后端集成与 API 证据已完成；真实 Compose + MySQL + Chromium 执行已完成。

| Requirement / acceptance | Implementation | Integration | E2E | 状态 |
|---|---|---|---|---|
| REQ02 profile update and re-query | `UserServiceImpl.updateCurrentUserProfile` | profile assertions in `ProfileAddressUc02IntegrationTest` | profile reload | 后端与浏览器已完成 |
| REQ02 address create/update/delete | `UserServiceImpl` address methods | same test | address page reload and delete refresh | 后端与浏览器已完成 |
| REQ02 max one default per owner | `clearDefaultAddress` | database count assertion | response assertion | 后端与浏览器已完成 |
| REQ02 cross-user isolation and unauthenticated rejection | `getOwnedAddress`, JWT interceptor | ownership test | API-assisted E2E | 已完成 |
