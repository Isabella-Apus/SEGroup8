# UC12 订单支付与取消测试报告

## 测试目标

验证仅待付款订单可支付，支付金额与优惠分摊守恒，余额不足时事务回滚，未付款取消恢复库存和优惠券，以及权限、幂等和支付/取消竞态。

## 自动化覆盖

| 场景 | 自动化证据 | 核心断言 |
|---|---|---|
| 多商品商城币支付 | `coinPaymentSplitsDiscountAndPreservesAccountAndLedgerConservation` | 订单拆分后的原价、优惠、卖家承担、平台承担和应付金额分别守恒；买家余额和流水一致 |
| 支付状态边界 | `onlyPendingPaymentOrderCanBePaid` | 已支付和已完成订单拒绝支付且资金、流水、状态不变 |
| 余额不足 | `insufficientBalanceRollsBackOrderBalanceVoucherAndLedger` | 订单、余额、优惠券和流水全部不变 |
| 未付款取消 | `unpaidCancellationRestoresStockAndReleasesVoucher` | 订单关闭、库存恢复、优惠券释放 |
| 已付款/已完成取消边界 | `paidCancellationNeverUsesUnpaidRestoreRules_andCompletedOrderIsRejected` | 已付款取消不执行未付款回滚；已完成订单拒绝取消 |
| 非买家操作 | `nonBuyerCannotPayOrCancel_andNoStateChanges` | 返回 403 且订单和库存不变 |
| 幂等与并发 | `duplicatePaymentAndCancellationReplayWithoutDuplicateSideEffects`、`concurrentPayAndCancelAllowsAtMostOneBusinessSideEffect` | 重放响应一致；支付/取消竞态仅一次产生业务副作用 |
| 浏览器真实流程 | `frontend/e2e/domain-c/uc12-pay-cancel.spec.ts` | 支付成功链和取消成功链完成后刷新，状态保持持久化 |

## 验证命令

```powershell
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs --suite UC12 --goal verify --maven-repository backend/.m2repo
```

后端定向测试使用 Testcontainers MySQL 8.4.6 和生产 `schema.sql`，不是 H2。实际结果：8 tests passed，0 failures，0 errors。

浏览器测试使用专用 Compose MySQL、后端服务和 Vite 前端。原始 Surefire、Playwright、日志、截图和汇总分别位于 `evidence/raw-reports/`、`evidence/logs/`、`evidence/screenshots/` 和 `evidence/result-summary.json`。

## 回归状态

UC12 定向 MySQL Integration 已通过。全后端 `clean verify`、前端 `build:real`、CI 和非作者 Review 应在 PR 合并前记录到 PR 正文，不以本地定向结果代替。
