# UC22 追溯

| 需求 | 代码与表 | 自动化测试 | 证据 | 状态 |
|---|---|---|---|---|
| 正常领取与重复领取拒绝 | `VoucherController.claim`、`voucher`、`user_voucher` | `VoucherClaimUc22IntegrationTest` | `04_tests/UC22/evidence/` | PASS |
| 不可领取状态与余量 | `VoucherService.claim` | `VoucherClaimUc22IntegrationTest` | 同上 | PASS |
| 店铺门槛与失败不占用 | `VoucherService.occupyForOrder` | `UC22VoucherThresholdTest`、`VoucherCheckoutUc22IntegrationTest` | 同上 | PASS |
| 取消释放与支付核销 | `releaseForCanceledOrder`、`markUsedForPaidOrder` | `VoucherCheckoutUc22IntegrationTest` | 同上 | PASS |
| 支付状态竞争与重放 | `OrderServiceImpl.payMyOrder`、订单版本号、幂等记录 | `OrderPayCancelUc12IntegrationTest` | `04_tests/UC12/` | PASS，共享证据 |
| 浏览器领券结算闭环 | 领券、商品、订单详情页面 | `uc22-claim-checkout.spec.ts` | `04_tests/UC22/evidence/` | PASS |
