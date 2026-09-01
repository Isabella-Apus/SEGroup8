# benefits-finance-service 架构交付

`benefits-finance-service` 是优惠券与资金域的唯一事实来源，覆盖 UC21–UC23，并向订单域的 UC12 支付、UC14 退款提供内部契约。

服务拥有 `benefits_finance_db`，通过本地事务同时更新余额、资金流水、请求状态和 Outbox；订单服务只保存稳定 ID 与结果快照，不得直接访问本 Schema。

## 本地验证

```bash
mvn -B -f microservices/pom.xml -pl benefits-finance-service -am clean verify
```

公开接口使用 identity-governance-service 签发的 Bearer JWT。内部接口必须提供 `X-Internal-Service-Token`，不接受浏览器 JWT 代替服务身份。

## 契约入口

- [OpenAPI](openapi.yaml) 给出全部公开/内部路径、请求与成功响应模型、统一错误响应、状态码、事件 schema 和错误码扩展目录。
- [跨服务调用](cross-service-calls.md) 给出幂等键、超时后的查询策略、Outbox 至少一次投递、事件头/正文、运行配置来源以及探针和日志字段。
- 金额以两位小数存储并随响应/日志记录币种；资金流水允许正负金额，余额与报价金额不得为负。

## 配置与可观测性摘要

运行时数据库、JWT 和内部令牌来自 Kubernetes Secret；TTL、币种、Outbox 目标、HTTP 超时和版本来自 ConfigMap。生产迁移应使用 `FLYWAY_DB_USERNAME`/`FLYWAY_DB_PASSWORD` 与运行时 DML 账号分离。当前 JWT 实现使用 `JWT_SECRET`，未实现 `JWT_PUBLIC_KEY` 模式。

服务暴露 `/actuator/health/liveness`、`/actuator/health/readiness` 和 `/actuator/info`。readiness 包含数据库、成功迁移/核心表只读以及密钥格式检查；日志包含关联 ID，并仅记录掩码后的用户 ID。配置矩阵和字段级说明见 [跨服务调用](cross-service-calls.md)。这些是交付契约，不代表尚未执行的 Kubernetes 故障演练、Helm 发布或回滚已完成。

相关材料：

- [服务边界](service-boundary.md)
- [数据归属](database-ownership.md)
- [跨服务调用、事件和错误码](cross-service-calls.md)
- [OpenAPI 完整契约](openapi.yaml)
- [追溯矩阵](traceability.md)
- [交付清单](delivery-manifest.md)
