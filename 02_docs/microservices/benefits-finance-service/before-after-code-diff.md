# MS-05 改造前后代码差异

## 可复现基线

- 改造前基线：`bb72290cff96c78ab189468b82db1f8ba3cd9323`
- 改造后基线：`main@b622e6bbb0447d6823b50e7789e4777f7131eb9b`，独立和完整系统流水线均通过。

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

Finance 的 Service DNS 是 `benefits-finance:8085`；订单和消息服务分别为 `segroup8-order:8085`、`messaging:8084`。当前生产 Ingress 已把 `/api/finance`、`/api/voucher` 切到 Finance，Order/Messaging 也已部署；独立 Finance E2E、Domain E 浏览器 E2E 和生产 rollout 均由当前服务流水线验证。兼容后端保留旧路由只用于回退与改造前比较。

## 代码层差异重点

- HTTP 写操作支持持久化 `Idempotency-Key` 指纹与响应重放；业务 request ID 保留用于金融事实查询。
- Flyway 迁移使用独立 migrator，运行账号只获 DML；MySQL Testcontainers 断言运行账号不能 DDL 或跨 schema 写入。
- Outbox 产出 messaging 兼容 `EventEnvelope`（`PaymentCompleted.v1` / `RefundCompleted.v1`），包含通知字段和 request/trace 关联 ID；Relay 同时传递相应 HTTP 头。
