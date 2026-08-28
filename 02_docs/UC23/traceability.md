# UC23 追溯

| REQ | SYS / COMP / OBJ | Controller / Service / Mapper / Table | UNIT / API / Integration / E2E | Report / Evidence | 状态 |
|---|---|---|---|---|---|
| 首次查询初始化；充值只进个人账户 | `system.mmd`、`component.mmd`、`object.mmd` | `FinanceController`、`EscrowSettlementService`、`BalanceMapper`、`balance` | `FinanceControllerUc23WebMvcTest`、`FinanceSettlementUc23IntegrationTest`、`uc23-wallet-settlement.spec.ts` | `04_tests/UC23/test-report.md`、`evidence/` | PASS |
| 个人与经营流水字段正确 | 同上 | `TransactionRecordMapper`、`transaction_record` | `UC23AccountIsolationTest`、UC23 Controller/Integration/E2E | 同上 | PASS |
| 普通用户不能查看经营流水；用户数据隔离 | 同上 | `FinanceController`、`UserMapper`、JWT `UserContext` | UC23 Controller/Integration/E2E | 同上 | PASS |
| 流水失败时余额回滚 | 同上 | `EscrowSettlementService`、Spring Transaction、`balance`、`transaction_record` | `FinanceSettlementUc23IntegrationTest.transactionRecordFailureRollsBackTheBalanceUpdate` | Surefire XML | PASS |
| 并发更新不丢金额 | 同上 | `EscrowSettlementService`、`balance.version` | `FinanceSettlementUc23IntegrationTest.concurrentPersonalCreditsDoNotLoseMoneyOrLedgerRows` | Surefire XML | PASS |
| 商家结算只进经营账户；重复结算幂等 | 同上 | `OrderController`、`OrderServiceImpl`、`NewProductSettlementStrategy` | UC23 Integration/E2E | Playwright JSON/XML、截图 | PASS |
| 退款后个人与经营账户守恒 | 同上 | `OrderServiceImpl`、`EscrowSettlementService` | `FinanceSettlementUc23IntegrationTest.refundMovesTheSettledAmountBackWithoutChangingTheCombinedBalance`、`OrderRefundUc14IntegrationTest` | Surefire XML | PASS |
