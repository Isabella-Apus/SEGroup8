# UC13 测试计划

## 验收目标

验证“发货、物流、收货和完成”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `UNIT-TC13-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/OrderServiceImplTest.java#shipSellerOrder_shouldPersistNotificationForBuyer` | 发货后通知买家 |
| `INT-TC13-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/NewProductFulfillmentUc13IntegrationTest.java#sellerShipsNewProduct_createsOneInitialTrace_andBothPartiesCanQuery`; `#nonSellerAndNonPendingShipAreRejected_andMergedOrderIsUnchanged`; `#receiveSettlesOnce_andConcurrentManualAutomaticConfirmationCannotDuplicateLedger`; `#repeatedShippingDoesNotCreateAnotherInitialTrace` | 发货、物流、收货结算、权限和幂等一致 |
| `E2E-TC13-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-c/uc13-fulfillment.spec.ts#seller ships, buyer views logistics and confirms receipt` | 发货、物流、收货和状态持久化均在真实栈验证，失败为非零退出码并保留原始证据 |

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC13/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC13/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-c/uc13-fulfillment.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
