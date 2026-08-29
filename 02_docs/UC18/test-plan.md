# UC18 测试计划

## 目标

验证二手议价从申请到确认/拒绝的资格、金额、重复、权限、并发、事务一致性和辅助系统故障隔离。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC18-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/SecondhandTradeServiceImplTest.java#confirmBargain_shouldCreatePendingPayOrderWhenSellerAccepts`; `#rejectBargain_shouldUpdateStatusAndNotifyBuyer`; `#rejectBargain_shouldThrowWhenAlreadyHandled` | 确认建单、拒绝通知和重复处理边界。 |
| `MVC-TC18-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/SecondhandTradeControllerUc18WebMvcTest.java#bargainApplyConfirmAndReject_shouldExposeStableRoutes` | 议价申请、确认和拒绝路由契约。 |
| `INT-TC18-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/SecondhandNegotiationIntegrationTest.java#applicationPersistsAndBothParticipantsCanListIt`; `#invalidNonNegotiableSelfAndRepeatedApplicationsAreRejected`; `#unrelatedSellerCannotConfirmOrReject`; `#confirmationCreatesOnePendingPaymentOrderAtConfirmedPrice`; `#rejectionEndsApplicationWithoutCreatingOrder`; `#concurrentConfirmAndRejectProduceExactlyOneDecision`; `#chatAndNotificationFailuresDoNotRollbackCoreDecision` | 申请、权限、并发、状态、建单和辅助系统故障隔离。 |
| `E2E-TC18-001` | Browser E2E | `frontend/e2e/domain-d/uc18-bargain.spec.ts#buyer applies, seller confirms in chat, and buyer receives a pending-payment order` | 真实页面申请、聊天确认、刷新和待付款订单校验。 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress -Dtest=SecondhandNegotiationIntegrationTest test
mvn -B --no-transfer-progress "-Dtest=SecondhandTradeServiceImplTest,SecondhandTradeControllerUc18WebMvcTest" test

cd ..\frontend
npx playwright test e2e/domain-d/uc18-bargain.spec.ts --workers=1
```

完整真栈由 `scripts/e2e/run-compose-e2e.ps1` 按 database -> backend -> frontend 顺序启动，并收集浏览器报告、截图和三端日志。
