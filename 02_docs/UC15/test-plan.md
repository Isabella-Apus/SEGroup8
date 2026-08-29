# UC15 测试计划

## 验收目标

验证“评价、追评和卖家回复”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `UNIT-TC15-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/ReviewServiceTest.java#orderReview_writesOneOriginalForEveryOrderItem_andCompletesOrder`; `#duplicateOriginalReview_isRejected`; `#invalidItemInBatch_isRejectedBeforeAnyInsert_andOrderStaysPendingReview` | 首评批量、重复和事务边界 |
| `MVC-TC15-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/ReviewControllerWebMvcTest.java#sellerReply_requiresOwnershipAndValidContent`; `#myReviews_areScopedToCurrentUser`; `#sellerReviews_withoutOwnedProducts_returnsEmptyWithoutQueryingUnscopedReviews` | 回复权限、当前用户范围和空结果契约 |
| `INT-TC15-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/ReviewFlowIntegrationTest.java#buyerOriginal_sellerReply_buyerFollowup_persistsAndAllowsOnlyOneFollowup`; `#secondItemDatabaseFailure_rollsBackFirstReview_andLeavesOrderPendingReview`; `#buyerAndSellerPagination_filterBeforePaging_andNeverLeakOtherUsersData` | 首评/回复/追评、事务回滚、分页和数据隔离 |
| `E2E-TC15-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-c/uc15-review.spec.ts#buyer reviews, seller replies, buyer follows up and state persists` | 首评、卖家回复、追评和刷新回读均在真实栈验证，失败为非零退出码并保留原始证据 |

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC15/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC15/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-c/uc15-review.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
