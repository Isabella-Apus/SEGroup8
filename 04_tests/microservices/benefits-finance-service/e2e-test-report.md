# E2E 测试报告

| 用例 | Spec | 本地状态 | 说明 |
|---|---|---|---|
| UC21 | `uc21-voucher-lifecycle.spec.ts` | PASS | 最新主线的真实 UI 平台栈完成卖家创建、更新、列表和关闭 |
| UC22 | `uc22-claim-checkout.spec.ts` | PASS | 最新主线的真实 UI 平台栈完成领券与已支付订单结算 |
| UC23 | `uc23-wallet-settlement.spec.ts` | PASS | 最新主线的真实 UI 平台栈完成充值与商家持久化经营流水 |
| UC12 | UC23 spec 中的 debit/result 协作断言 | PASS | 与支付协作契约同轮验证，不复制 Domain C spec |
| UC14 | UC23 spec 中的 refund/outbox 协作断言 | PASS | 与退款协作契约同轮验证，不复制 Domain C spec |

执行环境：Docker Desktop 4.67.0 / Engine 29.3.1；共享 Compose 的 `frontend`、`backend`、`database` 均为 `healthy`；目标地址 `http://127.0.0.1:8088`。

候选镜像 `segroup8/benefits-finance:e2e` 由已通过测试的唯一 Boot JAR 构建，修订为 `eca9b25a2c0ad6a17ad813446b67a23fcd72fcb0`，JAR SHA-256 为 `29e926346b399e2fc40073fc1c40779180cda535aecb515eca4b027c570821b9`，容器用户为 `10001:10001`。

- 独立服务 API E2E：候选镜像 + 独立 MySQL 8.4.6 + 严格事件 stub，3 passed、0 failed、0 skipped，1.5 秒；原始 JSON/JUnit 在 `evidence/independent-e2e/`。
- Domain E Compose E2E：健康的 frontend/backend/database 平台栈，3 passed、0 failed、0 skipped，16.4 秒；用于验证与订单、身份治理、二手服务共用主线的真实 UI 兼容性。该栈未将浏览器流量路由至候选服务，因此候选服务能力以独立 API E2E 为准；原始 JSON/JUnit 和关键日志在 `evidence/compose-e2e/`。

真实 Kubernetes 路由和 rollout 未执行，状态为 `NOT_RUN`；本地候选镜像 Compose 结果不冒充集群证据。
