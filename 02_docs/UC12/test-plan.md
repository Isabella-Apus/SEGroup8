# UC12 测试计划

## 验收目标

验证“支付和取消”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `UNIT-TC12-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/OrderServiceImplTest.java#cancelMyOrder_shouldRestoreStockWhenUnpaidEvenIfOrderStatusNotPendingPay` | 未付款取消恢复库存 |
| `UNIT-TC12-002` | Unit | `backend/src/test/java/com/segroup8/platform/interceptor/IdempotencyInterceptorTest.java#shouldAllowFirstRequestAndReplayDuplicateResult`; `#shouldReturnSuccessEnvelopeWhenDuplicateStillProcessing`; `#shouldIgnoreWhenHeaderMissing` | 幂等首请求、处理中回放和缺失 header |
| `INT-TC12-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/OrderPayCancelUc12IntegrationTest.java#coinPaymentSplitsDiscountAndPreservesAccountAndLedgerConservation`; `#onlyPendingPaymentOrderCanBePaid`; `#insufficientBalanceRollsBackOrderBalanceVoucherAndLedger`; `#unpaidCancellationRestoresStockAndReleasesVoucher`; `#paidCancellationNeverUsesUnpaidRestoreRules_andCompletedOrderIsRejected`; `#nonBuyerCannotPayOrCancel_andNoStateChanges`; `#duplicatePaymentAndCancellationReplayWithoutDuplicateSideEffects`; `#concurrentPayAndCancelAllowsAtMostOneBusinessSideEffect` | HTTP、数据库状态、账户/流水、优惠券、权限和并发边界一致 |
| `E2E-TC12-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-c/uc12-pay-cancel.spec.ts#pays and cancels persisted orders through the browser` | 支付、取消和状态持久化均在真实栈验证，失败为非零退出码并保留原始证据 |

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC12/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC12/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-c/uc12-pay-cancel.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
