# benefits-finance-service 运维入口

- 日常部署、日志、探针、版本、回滚与资金对账：[operations-runbook.md](operations-runbook.md)

运行时 Secret：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`INTERNAL_SERVICE_TOKEN`。配置项包括币种、HTTP 超时、报价/预占 TTL 和 Outbox 参数。

任何日志、截图或报告均不得包含完整 JWT、内部服务令牌、数据库口令或完整账户标识。
