# UC03 traceability

| Requirement / acceptance | Implementation | Integration | E2E |
|---|---|---|---|
| REQ03 submit and query own application | `MerchantApplicationServiceImpl.submit/getMyApplication` | main UC03 test | submit and query |
| REQ03 admin-only review | `pageForAdmin/approve/reject` | admin token + page assertions | API-assisted full-stack |
| REQ03 approve role/shop/audit linkage | `approve`, `AdminAuditLogService` | persisted app/user/shop/notification/audit checks | profile reload |
| REQ03 reject reason and unchanged role | `reject` | rejected branch assertions | rejected API path |
| REQ03 notification failure isolation | `persistNotificationSafely` | fault-injection integration test | documented as backend gate |
