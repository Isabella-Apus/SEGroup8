# UC16-UC20 追溯矩阵

| 用例/条件 | 实现 | 自动测试 | E2E 引用 |
|---|---|---|---|
| UC16 发布、审核、查询、所有权 | `ProductApplicationService`、`SecondhandProductController` | `SecondhandProductApiTest` | `frontend/e2e/domain-d/uc16-product-management.spec.ts` |
| UC17 禁止自购、CAS 直购、幂等订单 | `TradeApplicationService.buy` | `DirectBuyConcurrencyIntegrationTest` | `uc17-direct-purchase.spec.ts` |
| UC18 申请、拒绝、同意、待付款订单 | `applyBargain/confirmBargain/rejectBargain` | `BargainOrderIntegrationTest` | `uc18-bargain.spec.ts` |
| UC19 并发出价、流拍、可重入结算 | `placeBid/settleAuction` | `AuctionSettlementIntegrationTest` | `uc19-auction.spec.ts` |
| UC20 状态协作，不复制订单状态机 | `OrderStatusProjectionService` | 事件幂等与架构边界测试 | `uc20-fulfillment.spec.ts` |
| order 停止/延迟 | `TradeOrderCoordinator` | `OrderFailureRecoveryIntegrationTest` | 故障报告 |
| MySQL 独立 schema/跨库拒绝 | Flyway V1 | `MySqlSchemaOwnershipIntegrationTest` | 不适用 |
| 内部订单契约 | `HttpOrderGateway` | `HttpOrderGatewayContractTest` | 不适用 |
