# UC23 测试计划

目标：验证钱包初始化、个人充值、经营结算、流水字段、权限隔离、事务回滚、并发更新、重复结算和退款守恒。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC23-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/settlement/UC23AccountIsolationTest.java#unitUc23001_personalAndBusinessAccountsRemainIsolated` | 个人账户和商家账户余额/流水保持隔离。 |
| `UNIT-TC23-002` | Unit | `backend/src/test/java/com/segroup8/platform/service/settlement/EscrowSettlementServiceTest.java#sellerVoucherCreditsSellerWithGrossAmountMinusDiscount`; `#platformVoucherCreditsSellerWithFullGrossAmount` | 店铺券按扣除折扣后的金额给卖家结算，平台券按完整成交额结算。 |
| `MVC-TC23-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/FinanceControllerUc23WebMvcTest.java#dashboardReturnsBothIsolatedBalancesForCurrentUser`; `#rechargeCreditsOnlyThePersonalAccount`; `#invalidRechargeIsRejectedBeforeTheServiceWrites`; `#businessRecordsRequireOfficialSellerAndKeepTheirTradeMetadata` | 钱包看板、充值参数、账户写入范围和商家交易元数据正确。 |
| `INT-TC23-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/FinanceSettlementUc23IntegrationTest.java#walletInitializesRechargesAndReturnsOnlyTheOwnersPersonalRecords`; `#confirmedNewProductOrderSettlesOnceIntoTheSellerBusinessAccount`; `#transactionRecordFailureRollsBackTheBalanceUpdate`; `#concurrentPersonalCreditsDoNotLoseMoneyOrLedgerRows`; `#refundMovesTheSettledAmountBackWithoutChangingTheCombinedBalance` | 充值、交易结算、故障回滚、并发流水和退款回退在真实数据库中守恒且不重复。 |
| `E2E-TC23-001` | Browser E2E | `frontend/e2e/domain-e/uc23-wallet-settlement.spec.ts#buyer recharges and seller receives one persisted business settlement` | 真实页面完成充值并验证卖家商家账户仅产生一笔持久化结算。 |

运行命令：

```powershell
cd backend
mvn.cmd -B --no-transfer-progress -Dgroups=UC23 test
mvn.cmd -B --no-transfer-progress -Dgroups=DOMAIN_E test

cd ..\frontend
$env:E2E_BASE_URL = 'http://127.0.0.1:8088'
$env:E2E_OUTPUT_DIR = '..\04_tests\UC23\evidence\raw-reports\playwright'
npx.cmd playwright test e2e/domain-e/uc23-wallet-settlement.spec.ts --workers=1
```

Playwright 使用 `frontend/playwright.config.ts`、团队共享 fixture 和当前 Compose 环境。测试账号取本地非生产 seed 或对应 `E2E_*` 环境变量。
