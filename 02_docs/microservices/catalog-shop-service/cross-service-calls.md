# 跨服务调用与失败语义

| 协作 | 幂等/事务 | 失败处理 |
|---|---|---|
| `MerchantApproved.v1` | `applicationId` 唯一，`eventId` 写 `idempotency_record` | 消费端重试；重复事件返回同一店铺；持续失败进入 DLQ |
| 订单库存预留 | `X-Idempotency-Key` 唯一；商品行 `FOR UPDATE`；多项同事务 | 超时按幂等键恢复；订单失败调用 release；定时任务释放过期项 |
| 商品风险审核 | 提交与审核单同事务；无 LLM key 时规则结果确定 | 命中高风险规则时保持 `PENDING_REVIEW`；无命中时确定性自动通过并记录原因 |
| 审核/过期通知 | 本地结果与 `outbox_event` 同事务；使用 `EventEnvelope v1`、`X-Internal-Service-Token` | messaging 故障不回滚业务；发布器带连接/读取超时、指数退避、DEAD 状态和错误记录 |

内部 HTTP 必须携带 `X-Internal-Service-Token`。日志只记录 eventId/reservationId/requestId/traceId，不记录 token、JWT、密钥或请求正文。
