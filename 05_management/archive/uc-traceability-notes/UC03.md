# UC03 traceability

状态：后端集成与 API 证据已完成；真实 Compose + MySQL + Chromium 执行已完成。

| Requirement / acceptance | Implementation | Integration | E2E | 状态 |
|---|---|---|---|---|
| REQ03 submit and query own application | `MerchantApplicationServiceImpl.submit/getMyApplication` | main UC03 test | submit and query | 后端与浏览器已完成 |
| REQ03 admin-only review | `pageForAdmin/approve/reject` | admin token + page assertions | API-assisted full-stack | 后端与浏览器已完成 |
| REQ03 approve role/shop/audit linkage | `approve`, `AdminAuditLogService` | persisted app/user/shop/notification/audit checks | profile reload | 后端与浏览器已完成 |
| REQ03 reject reason and unchanged role | `reject` | rejected branch assertions | rejected API path | 后端与浏览器已完成 |
| REQ03 notification failure isolation | `persistNotificationSafely` | fault-injection integration test | documented as backend gate | 已完成 |
