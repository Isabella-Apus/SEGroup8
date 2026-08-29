# UC22 测试计划

目标：验证领券边界、店铺门槛、订单金额、支付核销和刷新持久化。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC22-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/UC22VoucherThresholdTest.java#unitUc22001_shopSubtotalBelowThresholdMustNotOccupyVoucher` | 店铺小计未达到门槛时不得占用优惠券。 |
| `UNIT-TC22-002` | Unit | `backend/src/test/java/com/segroup8/platform/service/VoucherServiceTest.java#occupyForOrder_shouldCalculateSellerDiscountFromMatchingShopSubtotal` | 按匹配店铺小计计算卖家券折扣。 |
| `INT-TC22-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/VoucherClaimUc22IntegrationTest.java#claimIsPersistedAndDuplicateClaimIsRejected`; `#closedNotStartedEndedAndSoldOutVouchersDoNotCreateUserVoucher` | 领券持久化/幂等以及关闭、未开始、过期、售罄状态拒绝。 |
| `INT-TC22-002` | Integration | `backend/src/test/java/com/segroup8/platform/integration/VoucherCheckoutUc22IntegrationTest.java#claimedVoucherIsUsedOnceByPaidOrder`; `#failedCheckoutDoesNotOccupyVoucherAndCanceledOrderReleasesIt` | 已领取券只能支付核销一次；结算失败不占券，取消释放占用。 |
| `E2E-TC22-001` | Browser E2E | `frontend/e2e/domain-e/uc22-claim-checkout.spec.ts#buyer claims and uses a voucher in a paid order through the real UI` | 真实页面完成领券、结算、支付和订单回读。 |

浏览器测试使用 `seller` 创建一次性测试券，使用 `user` 在页面完成业务动作。测试通过环境变量读取账号，不在仓库保存密码或 token。

运行命令：

```bash
cd backend && mvn -B --no-transfer-progress -Dgroups=UC22 test
E2E_EVIDENCE_ROOT=04_tests/UC22/evidence \
E2E_OUTPUT_DIR=04_tests/UC22/evidence/raw-reports/playwright \
scripts/e2e/run-compose-e2e.sh e2e/domain-e/uc22-claim-checkout.spec.ts --workers=1
```
