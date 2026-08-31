# 跨服务调用与恢复

| 调用 | 幂等键 | 超时/失败策略 |
|---|---|---|
| catalog reserve | `reservation:<client-key>` | 创建停止；已预留而本地失败则 release，失败写 Saga |
| catalog confirm/release | reservationId + action | 幂等补偿任务重试 |
| finance quote | `quote:<client-key>` | 创建返回 `FINANCE_QUOTE_UNAVAILABLE` |
| finance debit | `payment:<client-key>` | 超时只查询 requestId；未知保持 `PAYMENT_PENDING` |
| finance refund | `refund:<client-key>` | 超时只查询 requestId；未知保持 `REFUND_PENDING` |
| finance settlement | `settlement:<client-key>:<sellerId>` | 确认收货前按 requestId 查询未知结果；成功后才提交 `RECEIVED`，重复请求不重复结算 |
| finance voucher release | `voucher-release:<client-key>` | 与库存分别补偿，全部成功后取消完成 |
| messaging | outbox eventId | 异步投递失败不回滚订单 |
| catalog inventory event → order `/internal/events` | catalog `eventId` | `inbox_event` 去重；仅将仍为 `PENDING_PAY` 的对应订单转为 `CANCELLED`，已支付或已取消订单不回退 |

端点和请求/响应字段以 [openapi.yaml](openapi.yaml) 与消费者契约测试为准。`DownstreamHttpContractTest` 使用真实本地 HTTP Server 校验 catalog、finance 的路径、内部 token、幂等头与响应解析；secondhand 入站契约及 catalog 库存过期/释放事件的幂等恢复由 `OrderApiTest` 校验。HTTP 客户端没有支付、退款或结算自动重试器。
