# UC14 测试计划

## 验收目标

验证“退款、退货及仲裁”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `UNIT-TC14-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/OrderServiceImplTest.java#approveRefundBySeller_shouldPersistDecisionUserAndRemark` | 卖家审核记录 |
| `MVC-TC14-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/AdminOrderControllerWebMvcTest.java#approveRefund_shouldFailWhenRemarkTooLong`; `#afterSaleLogs_shouldReturnSuccessList` | 备注校验和售后日志查询契约 |
| `INT-TC14-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/OrderRefundUc14IntegrationTest.java#buyerApply_sellerReject_reapply_adminApprove_recordsCompleteOrderedAuditLog`; `#adminReject_recordsDecisionWithoutRefundSideEffects`; `#pendingShipOnlyRefund_autoApproves_refundsBuyer_andRestoresStock`; `#refundModes_enforceOnlyRefundAndReturnRefundBoundaries_withoutMutatingRejectedRequests`; `#buyerSellerAndAdminPermissions_rejectUnrelatedActors_andLeaveStateUnchanged`; `#settledRefund_returnsBuyerFunds_debitsSeller_andKeepsRefundLedgerConserved`; `#concurrentSellerAndAdminApproval_producesExactlyOneRefundAndOneApprovalLog` | 退款模式、权限、资金/库存、审计和并发幂等一致 |
| `INT-TC14-002` | Integration | `backend/src/test/java/com/segroup8/platform/integration/OrderAfterSaleIntegrationTest.java#buyerApply_thenAdminApprove_thenLogsShouldExist` | 兼容性审计回归 |
| `E2E-TC14-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-c/uc14-after-sale.spec.ts#buyer applies and seller approves, then admin arbitrates` | 退款申请、卖家审核、管理员仲裁和状态持久化均在真实栈验证，失败为非零退出码并保留原始证据 |

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC14/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC14/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-c/uc14-after-sale.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
