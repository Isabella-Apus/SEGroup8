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
| secondhand 创建订单（入站） | `orderBusinessKey` / `X-Idempotency-Key` | 校验商品、买卖双方和地址快照；同一业务键返回同一订单；统一响应 `{code,message,data:{orderId,orderNo,status}}` |
| secondhand 按业务键查单（入站） | `orderBusinessKey` | 用于创建响应丢失后的不确定结果查询；不存在返回 404，禁止消费者直接读 order 表 |

端点和请求/响应字段以 [openapi.yaml](openapi.yaml) 与消费者契约测试为准。`DownstreamHttpContractTest` 使用真实本地 HTTP Server 校验 catalog、finance 的路径、内部 token、幂等头与响应解析；secondhand 入站契约由 `OrderApiTest` 校验，消费者侧由 `HttpOrderGatewayContractTest` 校验同一信封和字段。HTTP 客户端没有支付、退款或结算自动重试器。
