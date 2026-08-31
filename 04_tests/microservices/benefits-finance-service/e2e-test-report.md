# E2E 测试报告

| 用例 | Spec | 本地状态 | 说明 |
|---|---|---|---|
| UC21 | `uc21-voucher-lifecycle.spec.ts` | 平台兼容 PASS / finance 路由 NOT_RUN | 旧平台 UI 栈完成卖家券流程；未切流到 finance 服务 |
| UC22 | `uc22-claim-checkout.spec.ts` | 平台兼容 PASS / finance 路由 NOT_RUN | 旧平台 UI 栈完成领券和订单结算；order-service 未接入当前 Compose |
| UC23 | `uc23-wallet-settlement.spec.ts` | 平台兼容 PASS / finance 路由 NOT_RUN | 旧平台 UI 栈完成充值和结算；未切流到 finance 服务 |
| UC12 | 独立 API E2E debit/result | PASS | 候选 finance 服务内部支付契约已验证 |
| UC14 | 独立 API E2E refund/outbox | PASS | 候选 finance 服务退款和严格 envelope 已验证 |

执行环境：Docker Desktop 4.67.0 / Engine 29.3.1；共享 Compose 的 `frontend`、`backend`、`database` 均为 `healthy`；目标地址 `http://127.0.0.1:8088`。

候选镜像 `segroup8/benefits-finance:e2e` 由已通过测试的唯一 Boot JAR 构建，tested revision 为 `cbc1ab300d0bf3c78669d1c0ac9f72fd5a390e3a`，JAR SHA-256 为 `c8dc7f93525fb8778adfa7b46d9adb55d08cd62caf488339b8a9842d362c957d`，镜像 ID 为 `sha256:2a60df31818f68f28b40471b9f69d55fa1d2537dbf5cbcab879df8a2516b1e9c`，容器用户为 `10001:10001`。

- 独立服务 API E2E：候选镜像 + 独立 MySQL 8.4.6 + 严格 messaging-compatible event stub，3 passed、0 failed、0 skipped，1.8 秒；原始 JSON/JUnit 在 `evidence/independent-e2e/`。
- Domain E Compose 回归：健康的 frontend/backend/database 旧平台栈，3 passed、0 failed、0 skipped，15.3 秒；仅作为平台兼容性回归。该栈没有 order-service、messaging-service 或 finance 前端/订单路由，因此归属 UC 的真实 finance 路由浏览器 E2E 为 `NOT_RUN`；原始 JSON/JUnit 在 `evidence/compose-e2e/`。

真实 Kubernetes 路由和 rollout 未执行，状态为 `NOT_RUN`；本地候选镜像 Compose 或旧平台兼容结果不冒充集群或完整微服务路由证据。
