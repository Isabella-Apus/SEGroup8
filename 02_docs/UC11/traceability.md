# UC11 追溯矩阵

| 需求与验收条件 | 设计/代码 | 自动化场景 | Evidence |
|---|---|---|---|
| `AC11-01` 合法结算、重复项合并、服务端计价 | `OrderController#create`、`OrderServiceImpl#createOrder` | `createsOrderFromServerPrice_mergesItems_andPersistsResponseConsistently` | Surefire XML、后端日志、`result-summary.json` |
| `AC11-02` 库存、订单与优惠券事务一致 | `OrderServiceImpl`、`VoucherService#occupyForOrder` | `appliesEligibleVoucher_andKeepsOrderInventoryAndVoucherConsistent`、`laterItemPersistenceFailure_rollsBackOrderInventoryAndOccupiedVoucher` | H2/MySQL Surefire XML、数据库断言 |
| `AC11-03` 商品、地址、自购、拉黑、券和参数异常 | DTO Validation、`AddressMapper`、`ShopMapper`、`UserBlockMapper`、`VoucherService` | `rejectsInvalidProductAddressSelfPurchaseAndBlockRelationships_withoutSideEffects`、`rejectsInvalidRequestParameters_beforeWritingBusinessData`、`rejectsUnclaimedThresholdAndShopMismatchVouchers_andRollsBackEverything` | Surefire XML、后端日志 |
| `AC11-03` 相同幂等键仅产生一次副作用 | `IdempotencyInterceptor`、`IdempotencyResponseAdvice` | `duplicateIdempotencyKey_replaysResponseAndCreatesOnlyOneOrder` | HTTP 回放头、`idempotency_record` 断言 |
| `AC11-04` 购物车到订单详情并刷新持久化 | `CartView.vue`、`OrderDetailView.vue` | `uc11-checkout-order.spec.ts` | Playwright JSON/HTML、截图、Compose 日志 |

## 链接

- 需求：[UC11 需求](../../01_requirements/UC11-购物车结算与创建订单.md)
- 设计：[UC11 设计](README.md)
- 测试计划：[UC11 测试计划](../../04_tests/UC11/test-plan.md)
- 测试报告：[UC11 测试报告](../../04_tests/UC11/test-report.md)
