# benefits-finance-service 追溯

| UC | 接口 | 代码/测试 | 当前结果 |
|---|---|---|---|
| UC21 券生命周期 | `/api/voucher/seller/**`、`/admin/**` | `VoucherService`、公开 API/真实 MySQL、Domain E Playwright | PASS |
| UC22 领券与结算 | 列表、领券、quote/reserve/consume/release | `VoucherService`、幂等与并发测试、Domain E Playwright | PASS |
| UC23 钱包与结算 | dashboard/recharge/records、debit/refund/settlements | `FinanceService`、`FinanceAtomicityIntegrationTest`、独立候选 E2E、Domain E Playwright | PASS |

生产路由为 `/api/finance`、`/api/voucher`，独立流水线执行 Maven、真实 MySQL、全部公开 API、候选镜像 E2E、完整 Domain E、Helm 和 K3s rollout。基线提交 `b622e6...` 对应 Actions run `33526387386`；新提交以其实际 Actions 结果为准。
