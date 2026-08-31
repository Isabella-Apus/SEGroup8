# MS-05 追溯矩阵

| 需求/UC | API/实现 | 自动测试 | 证据 |
|---|---|---|---|
| UC21 券生命周期 | `/api/voucher/seller/**`、`/admin/**` | `PublicApiTest`、独立候选 API E2E、Domain E Playwright | 候选 API E2E PASS；旧平台 UI 兼容回归 PASS；finance 路由浏览器 E2E NOT_RUN |
| UC22 领券与结算 | `/list`、`/{id}/claim`、quote/reserve/consume/release | `PublicApiTest`、独立候选 API E2E、Domain E Playwright | 候选 API E2E PASS；旧平台 UI 兼容回归 PASS；finance 路由浏览器 E2E NOT_RUN |
| UC23 钱包与结算 | dashboard/recharge/records、debit/refund/settlements | `FinanceAtomicityIntegrationTest`、独立候选 API E2E、Domain E Playwright | 事务/并发 + 候选 API E2E PASS；finance 路由浏览器 E2E NOT_RUN |
| UC12 支付协作 | debit + GET result | Domain C `uc12-pay-cancel.spec.ts` + 资金集成测试 | 不复制 Domain C spec |
| UC14 退款协作 | refund + Outbox | Domain C 后端回归 + 资金集成测试 | 不复制 Domain C spec |
| 服务身份 | `RequestSecurityFilter` | `PublicApiTest.apiEnforces...` | 公开 JWT/内部 token |
| MySQL 契约 | Flyway V1/V2、独立 migrator 与运行 DML 账号 | `MySqlContractIntegrationTest` | Testcontainers MySQL 8.4.6 实际执行 PASS，运行账号 DDL/跨 schema 写入被拒绝 |

说明：`evidence/raw-reports/` 保存当前代码的 Surefire XML/TXT；`evidence/independent-e2e/` 保存独立候选镜像 API E2E；`evidence/compose-e2e/` 保存旧平台 Compose 的 Domain E UI 兼容回归。根 Compose 尚未含 order/messaging/finance 切流，完整微服务路由和目标 Kubernetes rollout 均为 `NOT_RUN`，不能用本地 Compose 结果替代。
