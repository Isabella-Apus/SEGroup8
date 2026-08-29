# UC20 测试计划

## 目标

验证二手成交订单从付款、卖家发货、物流可见、买家确认收货到卖家个人钱包结算的权限、状态、幂等、事务和故障隔离，并确认收货后的用户界面停留在待评价状态。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC20-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/OrderServiceImplTest.java#shipSellerOrder_shouldPersistNotificationForBuyer`; `#shipSellerOrder_shouldRejectLegacyMergedOrder` | 发货通知和旧合并订单兼容性。 |
| `MVC-TC20-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/OrderControllerUc20WebMvcTest.java#shipAndConfirmReceive_shouldExposeStableRoutes` | 发货/确认收货路由契约。 |
| `INT-TC20-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/SecondhandFulfillmentLifecycleIntegrationTest.java#shipmentRequiresSellerOwnershipPaymentAndPendingShipmentState`; `#repeatedShipmentIsIdempotentAndCreatesOneInitialTrace`; `#onlyBuyerCanConfirmAndRepeatedReceiptSettlesExactlyOnce`; `#notificationFailuresDoNotRollbackShipmentOrReceipt`; `#settlementFailureRollsBackReceiptAndRetryDoesNotDuplicateCredit`; `#bargainOrderCreationFailureRestoresNegotiationAndProduct` | 发货/收货权限、物流轨迹、结算、通知故障和事务回滚。 |
| `INT-TC20-002` | Integration | `backend/src/test/java/com/segroup8/platform/integration/SecondhandOrderFlowIntegrationTest.java#secondhandSellerCanViewShipAndPushLogistics` | 既有二手订单物流可见性回归。 |
| `E2E-TC20-001` | Browser E2E | `frontend/e2e/domain-d/uc20-fulfillment.spec.ts#seller ships, buyer sees logistics and confirms receipt with one settlement` | 真实页面履约、物流、收货、待评价和一次结算。 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress "-Dtest=SecondhandFulfillmentLifecycleIntegrationTest,OrderControllerUc20WebMvcTest,SecondhandOrderFlowIntegrationTest,OrderServiceImplTest" test
mvn -B --no-transfer-progress clean verify

cd ..
$env:DOMAIN_D_SUITE = 'UC20'
$env:E2E_EVIDENCE_ROOT = '04_tests/UC20/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC20/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-d/uc20-fulfillment.spec.ts --workers=1
```

统一脚本按 database -> backend -> frontend 启动，并等待 Docker healthcheck、后端 HTTP health 和前端 HTTP health。Playwright 失败必须返回真实非零退出码，阻断后续 deploy/release，并保留 HTML、JSON、JUnit、截图、trace、video 与 Compose 服务日志。
