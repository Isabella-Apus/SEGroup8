# MS-05 改造前后代码差异

## 可复现基线

- 改造前基线：`bb72290cff96c78ab189468b82db1f8ba3cd9323`（PR 目标 `main`）
- 改造后：运行 `git rev-parse HEAD` 获取当前 PR 提交；提交后重新运行验收并在 `result-summary.json` 记录 tested revision。

可复现命令：

```powershell
git diff --stat bb72290cff96c78ab189468b82db1f8ba3cd9323...HEAD
git diff -- microservices/benefits-finance-service deploy/helm/segroup8 .github/workflows/benefits-finance-service-ci-cd.yml
```

## 目录和模块

| 改造前 | 改造后 |
|---|---|
| 单体 `backend` 承担券和资金逻辑 | `microservices/benefits-finance-service` 独立 Spring Boot 模块、入口、配置、Flyway、Dockerfile、测试与独立 Compose |
| 单体表 | 独占 `benefits_finance_db`：voucher、user_voucher、balance、transaction_record、checkout_quote、payment_request、idempotency_record、outbox_event |
| 单体内调用 | `/internal/**` 服务令牌契约，业务 ID 查询、Outbox 最终一致性和关联 ID 传播 |
| 无独立交付门禁 | 独立命名 CI、候选镜像、MySQL Testcontainers、独立 API E2E、Helm lint/template 与部署脚本 |

## 路由和兼容阶段

finance 的目标 Service DNS 是 `benefits-finance:8085`；订单和消息下游目标分别为 `segroup8-order:8085`、`messaging:8084`。当前 `main` 的根 Compose 仍仅含旧单体 backend，订单/messaging 微服务尚未合入本 PR 的运行栈。因此本次保留旧单体兼容路由，归属 UC 的浏览器微服务切流明确为 `NOT_RUN`，不得把旧平台 UI 回归记为 finance 路由 E2E。

## 代码层差异重点

- HTTP 写操作支持持久化 `Idempotency-Key` 指纹与响应重放；业务 request ID 保留用于金融事实查询。
- Flyway 迁移使用独立 migrator，运行账号只获 DML；MySQL Testcontainers 断言运行账号不能 DDL 或跨 schema 写入。
- Outbox 产出 messaging 兼容 `EventEnvelope`（`PaymentCompleted.v1` / `RefundCompleted.v1`），包含通知字段和 request/trace 关联 ID；Relay 同时传递相应 HTTP 头。
