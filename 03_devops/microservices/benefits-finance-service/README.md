# benefits-finance-service 运维入口

- 日常部署、扩缩容、探针与回滚：[operations-runbook.md](operations-runbook.md)
- 数据库/服务故障演练：[deployment-failure-drill.md](deployment-failure-drill.md)
- 余额与流水对账：[reconciliation-runbook.md](reconciliation-runbook.md)

运行时 Secret：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`INTERNAL_SERVICE_TOKEN`。配置项：`ORDER_SERVICE_URL`、`CURRENCY`、HTTP 超时、报价和预占 TTL、Outbox 参数。

任何日志、截图或报告均不得包含完整 JWT、内部服务令牌、数据库口令或完整账户标识。
