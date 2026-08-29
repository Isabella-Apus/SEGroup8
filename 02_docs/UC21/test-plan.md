# UC21 测试计划

目标：验证店铺券和平台券的创建、修改、关闭、删除、归属、权限与参数边界。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC21-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/UC21VoucherLifecycleRulesTest.java#unitUc21001_discountMustNotExceedThreshold` | 优惠金额不得超过门槛规则。 |
| `UNIT-TC21-002` | Unit | `backend/src/test/java/com/segroup8/platform/service/VoucherServiceTest.java#create_shouldUseRealShopIdInsteadOfSellerUserId` | 创建优惠券使用真实店铺归属，而不是卖家用户 ID。 |
| `INT-TC21-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/VoucherLifecycleUc21IntegrationTest.java#sellerAndAdminManageOwnedVoucherLifecycle`; `#otherSellerCannotManageVoucherAndUsedVoucherCannotBeDeleted`; `#invalidRulesAreRejectedWithoutWritingVoucher`; `#closedVoucherCannotBeClaimed` | 卖家/管理员生命周期、归属权限、参数拒绝、已使用券删除边界和关闭券领取边界落库一致。 |
| `E2E-TC21-001` | Browser E2E | `frontend/e2e/domain-e/uc21-voucher-lifecycle.spec.ts#seller creates, edits and closes a voucher through the real UI` | 真实页面完成卖家创建、修改、关闭和刷新后回读。 |

边界断言包括：其他卖家不能修改不属于自己的券、已使用券不能删除、关闭券不能领取、普通买家不能调用管理员创建接口。

运行命令：

```bash
cd backend && mvn -B --no-transfer-progress -Dgroups=DOMAIN_E test
E2E_EVIDENCE_ROOT=04_tests/UC21/evidence \
E2E_OUTPUT_DIR=04_tests/UC21/evidence/raw-reports/playwright \
scripts/e2e/run-compose-e2e.sh e2e/domain-e/uc21-voucher-lifecycle.spec.ts --workers=1
```
