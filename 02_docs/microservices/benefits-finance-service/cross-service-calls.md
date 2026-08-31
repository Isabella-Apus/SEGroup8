# 跨服务调用与失败语义

| 调用 | 幂等键 | 成功事实 | 超时/失败处理 |
|---|---|---|---|
| 报价 | `Idempotency-Key` + `orderRequestId` | 固化 quoteVersion、金额和有效期 | 返回原报价；缺少快照时报明确不可结算原因 |
| 券预占/核销/释放 | `orderRequestId` | `user_voucher` 条件状态迁移 | 订单失败幂等释放；超时任务回收 |
| 扣款 | `Idempotency-Key` + `paymentRequestId` | 余额、负流水、请求、Outbox 同事务 | order-service 先 GET 查询，禁止盲重试新 ID |
| 退款 | `Idempotency-Key` + `refundRequestId` | 正向反向流水、余额、Outbox 同事务 | 重复返回原结果；累计退款不得超付 |
| 结算 | `orderId + sellerId` | 商家余额与结算流水同事务 | 并发只有一笔入账 |

内部调用必须设置：

- `X-Internal-Service-Token`：由 Kubernetes Secret 注入。
- `X-Request-Id`、`X-Trace-Id`：贯穿 order、finance 和消息消费端。
- 写操作可带 `Idempotency-Key`：同一调用方和路由下，同键同请求重放已持久化响应；同键不同请求返回 `409 IDEMPOTENCY_KEY_REUSED`。业务请求 ID 仍用于资金事实查询与超时恢复。
- 超时建议：连接 1 秒、读取 3 秒；超时后按业务请求 ID 查询结果。

Outbox 发布失败只影响事件可见性，不回滚已经提交的资金事实；发布器以 1–300 秒
指数退避重试，默认最多 8 次。次数耗尽后进入 `DEAD`，不会再被轮询器发送；运维先
按 `event_id` 查询消费方结果，再受控重新排队，避免无限重试和重复副作用。

## HTTP 身份与关联标识

| 调用面 | 身份凭据 | 结果 |
|---|---|---|
| `/api/**` | `Authorization: Bearer <JWT>` | JWT 缺失、无效或过期返回 `401 AUTH_REQUIRED`；角色不足返回 `403 ROLE_FORBIDDEN` |
| `/internal/**` | `X-Internal-Service-Token` | 缺失或不匹配返回 `403 SERVICE_IDENTITY_FORBIDDEN`；浏览器 JWT 不能替代服务身份 |

调用方应传递只含 `[A-Za-z0-9._:-]`、最长 80 字符的 `X-Request-Id` 和 `X-Trace-Id`。服务会在响应中返回最终采用的 `X-Request-Id`；不合法或缺失的关联 ID 会被替换为 UUID。不得把 JWT、内部令牌、数据库凭据或完整账户信息写入日志。

## 事件契约

资金事实与对应 Outbox 记录在同一本地事务中提交。Relay 通过 `OUTBOX_EVENT_SINK_URL` 发送 JSON，请求头为：

| 请求头 | 含义 |
|---|---|
| `X-Event-Id` | 全局唯一事件 ID；消费方以此去重 |
| `X-Event-Type` | `PaymentCompleted.v1` 或 `RefundCompleted.v1`；结算与充值使用 `PaymentCompleted.v1` 及不同通知文本 |
| `X-Internal-Service-Token` | 集群服务身份 |
| `X-Request-Id`、`X-Trace-Id` | 与 envelope 中的 request/trace ID 一致，供消费端日志关联 |

契约测试严格校验 HTTP `POST`、内部服务令牌、`X-Event-Id`、`X-Event-Type` 和关键
JSON 请求体，不使用“任意请求都返回 200”的宽松 stub。

事件正文结构：

```json
{
  "eventId": "...", "eventType": "PaymentCompleted.v1", "eventVersion": 1,
  "producer": "benefits-finance-service", "aggregateType": "PAYMENT", "aggregateId": "pay-9001-1",
  "occurredAt": "2026-08-31T00:00:00Z", "traceId": "trace-...",
  "payload": {
    "requestId": "request-...", "recipientUserId": 101, "displayTitle": "支付成功",
    "displayText": "订单支付已完成", "dedupeKey": "finance:pay-9001-1:PaymentCompleted.v1",
    "transactionId": "...", "orderId": 9001
  }
}
```

充值使用 `PaymentCompleted.v1` 且 `orderId` 为 `0`。投递语义是至少一次：HTTP 成功后标记 `PUBLISHED`；超时或非成功响应会恢复为 `PENDING` 并指数退避；`SENDING` 超过五分钟可被重新领取。因此消费方必须按 `X-Event-Id` 幂等，不能依赖仅一次投递。当前正文不包含金额或完整账户信息；消费者需要更多资金详情时，应使用受保护的内部查询契约，而不是解析日志。

## 错误码目录

所有错误响应使用统一结构 `code/message/requestId/timestamp`。`409` 表示业务事实未按本次请求发生；资金调用遇到超时或 `500` 时，应先用业务请求 ID 查询结果，禁止换新 ID 盲目重试。

| HTTP | 错误码 | 适用场景 |
|---|---|---|
| 400 | `INVALID_ARGUMENT` | Bean Validation、JSON 或参数类型不合法 |
| 400 | `SHOP_REQUIRED`、`ADMIN_SCOPE_INVALID`、`VOUCHER_TIME_INVALID`、`VOUCHER_QUANTITY_INVALID`、`VOUCHER_DISCOUNT_INVALID`、`VOUCHER_SCOPE_INVALID` | 券规则不合法 |
| 401 | `AUTH_REQUIRED` | 公开 API 缺少有效 JWT |
| 403 | `ROLE_FORBIDDEN`、`SERVICE_IDENTITY_FORBIDDEN` | 用户角色或服务身份不允许 |
| 403/409 | `VOUCHER_NOT_OWNED` | 发行方归属校验或用户可用券校验失败；以具体端点状态为准 |
| 404 | `VOUCHER_NOT_FOUND`、`PAYMENT_REQUEST_NOT_FOUND` | 目标事实不存在 |
| 409 | `IDEMPOTENCY_KEY_REUSED`、`DATA_CONFLICT` | 同一幂等键参数不一致或唯一约束冲突 |
| 409 | `VOUCHER_CONCURRENT_UPDATE`、`VOUCHER_ALREADY_CLAIMED`、`VOUCHER_SOLD_OUT`、`VOUCHER_NOT_RESERVABLE`、`VOUCHER_NOT_RESERVED`、`VOUCHER_ALREADY_USED`、`VOUCHER_RELEASE_CONFLICT`、`VOUCHER_NOT_USABLE`、`VOUCHER_THRESHOLD_NOT_MET`、`VOUCHER_SCOPE_MISMATCH`、`VOUCHER_CLOSED`、`VOUCHER_NOT_CLAIMABLE` | 券状态、库存、范围或条件冲突 |
| 409 | `PAYMENT_NOT_REFUNDABLE`、`PAYMENT_REFERENCE_MISMATCH`、`REFUND_EXCEEDS_PAYMENT`、`INSUFFICIENT_BALANCE`、`BALANCE_CONCURRENT_UPDATE` | 支付、退款或余额冲突 |
| 500 | `INTERNAL_ERROR` | 未预期失败；携带 `requestId` 排查并按业务请求 ID 查询 |

各端点完整成功模型、状态码和示例见 [openapi.yaml](openapi.yaml)。

## 运行配置契约

| 环境变量 | 来源 | 默认/约束 | 用途 |
|---|---|---|---|
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | Kubernetes Secret | 生产必须显式提供 | 运行时数据源，只连接 `benefits_finance_db` |
| `FLYWAY_DB_USERNAME`、`FLYWAY_DB_PASSWORD` | Kubernetes Secret | 未设置时回退运行时账号；生产应使用独立迁移账号 | Flyway DDL 身份 |
| `JWT_SECRET` | Kubernetes Secret | 至少 32 UTF-8 字节且不能含 `change-me`/`demo_secret` | 公开 JWT 校验；当前实现不接受 `JWT_PUBLIC_KEY` |
| `INTERNAL_SERVICE_TOKEN` | Kubernetes Secret | 至少 16 UTF-8 字节且不能含占位文本 | 内部 API 与 Outbox 服务身份 |
| `CURRENCY` | ConfigMap | `CNY` | 金额币种 |
| `QUOTE_TTL_SECONDS` | ConfigMap | `300` | 报价有效期 |
| `RESERVATION_TTL_SECONDS` | ConfigMap | `900` | 券预占有效期 |
| `ORDER_SERVICE_URL` | ConfigMap | `http://segroup8-order:8085` | 订单服务 Kubernetes DNS；当前 finance 仅保留调用契约，未在本 PR 中部署 order 服务 |
| `OUTBOX_EVENT_SINK_URL` | ConfigMap | `http://messaging:8084/internal/events` | messaging Kubernetes DNS；空值会暂停发布但保留 Outbox |
| `OUTBOX_POLL_MS`、`OUTBOX_BATCH_SIZE` | ConfigMap | `5000`、`50` | Relay 扫描周期和批量 |
| `HTTP_CONNECT_TIMEOUT_MS`、`HTTP_READ_TIMEOUT_MS` | ConfigMap | `1000`、`3000` | Outbox HTTP 连接/读取超时 |
| `APP_VERSION` | ConfigMap | 本地为 `dev`，部署应为不可变镜像 SHA 标签 | `/actuator/info` 版本 |

## 探针与日志字段

- liveness：`/actuator/health/liveness`，只用于判断进程是否存活。
- readiness：`/actuator/health/readiness`，组合 `readinessState`、`db`、`financeSchema`、`secrets`。`financeSchema` 检查成功 Flyway 历史及 `balance` 只读查询；`secrets` 检查必要密钥格式。
- version：`/actuator/info` 的 `app.name` 与 `app.version`。
- 基础日志 pattern 固定包含 `traceId`、`requestId` 和掩码后的 `userId`；报价日志补充 `quoteId`，资金完成日志按类型补充 `orderId`、`paymentRequestId` 或 `refundRequestId`、`transactionId`、两位小数金额和币种，Outbox 日志补充 `eventId/eventType`。

上述内容是接口与部署契约说明，不代表 Kubernetes 故障演练已经执行。真实 Pod events、探针失败、Helm revision/回滚及恢复后重复请求证据仍以测试与运维目录中的状态为准。
