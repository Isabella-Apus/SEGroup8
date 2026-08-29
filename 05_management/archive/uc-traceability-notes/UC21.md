# UC21 追溯

| REQ | SYS / COMP / OBJ | Code / Table | Test | Report / Evidence | 状态 |
|---|---|---|---|---|---|
| 卖家券生命周期与归属 | `system.mmd`、`component.mmd`、`object.mmd`、历史 #90 | `VoucherController`、`VoucherService`、`VoucherMapper`、`voucher` | `VoucherLifecycleUc21IntegrationTest` | `04_tests/UC21/test-report.md` | API/Integration PASS |
| 管理员平台券与角色隔离 | 同上 | `UserMapper`、`ShopMapper`、`voucher` | `VoucherServiceTest`、UC21 Integration | 同上 | PASS |
| 真实浏览器刷新持久化 | 同上 | `SellerVoucher.vue` | `uc21-voucher-lifecycle.spec.ts` | `04_tests/UC21/evidence/` | PASS |
