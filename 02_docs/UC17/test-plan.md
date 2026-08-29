# UC17 测试计划

## 目标

验证二手直接购买的状态、地址、并发、事务、取消、支付、重复提交和议价价格边界。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC17-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/SecondhandProductServiceImplTest.java#buySecondhandProduct_shouldThrowWhenBuyerIsSeller`; `#buySecondhandProduct_shouldCreateOrderWhenSuccess`; `#buySecondhandProduct_shouldGenerateUniqueOrderNumbersForRapidPurchases` | 自购拒绝、成功建单和订单号唯一。 |
| `MVC-TC17-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/SecondhandProductControllerUc17WebMvcTest.java#buy_shouldAcceptAddressAndReturnPendingOrder` | 地址参数和待付款响应契约。 |
| `INT-TC17-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/SecondhandDirectPurchaseIntegrationTest.java#availableProductAndOwnedAddressCreateOnePendingOrderAtomically`; `#offShelfSoldAndSelfOwnedProductsAreRejectedWithoutOrders`; `#missingAndForeignAddressesAreRejectedBeforeProductReservation`; `#twoConcurrentBuyersProduceExactlyOneOrderAndOneWinner`; `#orderInsertFailureRollsBackReservedProduct`; `#unpaidCancellationRelistsProductWhilePaymentMovesOrderToPendingShipment`; `#duplicateClickCreatesOneDealAndNegotiatedPriceHonorsEffectiveWindow` | 商品/地址状态、并发、事务、取消、支付和重复提交一致。 |
| `E2E-TC17-001` | Browser E2E | `frontend/e2e/domain-d/uc17-direct-purchase.spec.ts#creates one pending order through the real UI and rejects a repeated purchase` | 真实页面选地址、下单、刷新、订单一致性和重复购买拒绝。 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress -Dtest=SecondhandDirectPurchaseIntegrationTest test

cd ../frontend
npx playwright test e2e/domain-d/uc17-direct-purchase.spec.ts --workers=1
```

完整真栈由 `scripts/e2e/run-compose-e2e.ps1` 启动并收集失败证据。
