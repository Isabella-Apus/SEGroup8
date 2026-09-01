# 跨服务调用与恢复

## 创建二手订单

二手服务不读取身份库或订单库。创建订单前按以下固定顺序调用：

1. 直接购买调用 `GET http://identity-governance-service:8091/internal/users/{buyerId}/addresses/{addressId}`；议价和定时拍卖结算没有地址参数时调用 `GET /internal/users/{buyerId}/shipping-address` 取得默认优先的配送地址。两者都携带 `X-Internal-Service-Token` 和 `X-Request-Id`，并返回收件人、电话、省、市、详细地址。
2. 将第 1 步取得的地址与商品名称固化到本地 `trade_order_request`，再请求 `POST http://segroup8-order:8085/internal/orders/secondhand`；携带同一内部 Token，以及与请求体一致的稳定 `Idempotency-Key=orderBusinessKey`。请求只传完整快照，不传地址表 ID，不使用占位姓名、电话或地址。
3. 调用结果不确定时，以相同业务键请求 `GET /internal/orders/by-business-key/{key}`。只在查询明确返回订单不存在时重试创建；这类确定失败达到 `ORDER_MAX_ATTEMPTS` 后解除商品冻结并记录失败事件。如果查询本身不可用，则继续保持商品冻结和 `RETRY`，避免把“可能已经建单”误判成失败。

身份服务不可用或地址不属于买家时，本次建单保持 `RETRY`/最终 `FAILED`，不会用虚假地址继续。这是业务依赖降级，不影响二手服务自身 readiness；恢复任务会继续用相同业务键重试。

## 订单请求契约

```json
{
  "tradeType": "DIRECT_BUY|BARGAIN|AUCTION",
  "tradeId": "业务内唯一值",
  "buyerUserId": 20,
  "sellerUserId": 10,
  "productId": 1,
  "orderBusinessKey": "SECONDHAND:DIRECT_BUY:1-v1",
  "productName": "交易创建时的商品名称快照",
  "price": 75.00,
  "receiverName": "买家姓名",
  "receiverPhone": "13800000000",
  "receiverProvince": "浙江省",
  "receiverCity": "杭州市",
  "receiverDetailAddress": "详细地址",
  "remark": "可选"
}
```

地址 ID 不跨服务传给 order-service。secondhand-service 通过身份治理地址接口校验所有权后，
先把不可变地址快照和商品名写入自己的 `trade_order_request`，后续首次调用和恢复重试均复用该快照。
order-service 对 `orderBusinessKey` 唯一，并要求 `Idempotency-Key` 与其一致；正常响应统一为
`{code,message,data:{orderId,orderNo,status}}`，初始订单状态为 `PENDING_PAY`。

## 超时与补偿顺序

1. 本地事务将商品从 `ON_SHELF` CAS 为 `TRADE_PENDING`，同时写 `trade_order_request`。
2. 调用订单创建接口。
3. 调用异常时先执行 `GET /internal/orders/by-business-key/{key}`，防止“响应丢失但订单已创建”造成重复订单。
4. 查询无结果才把请求置为 `RETRY`；恢复任务按相同 business key 重试。`next_retry_at` 的写入和到期判断
   都使用数据库 `CURRENT_TIMESTAMP`，不能混用 JVM 本地时间与数据库时钟。
5. 达到 `ORDER_MAX_ATTEMPTS` 后置为 `FAILED` 并解除商品冻结。

身份治理的 4xx 地址所有权错误是终止性业务错误，不进入订单重试；身份治理或订单服务的连接失败、超时、
5xx 是暂时性错误。订单接口 4xx 表示契约或数据错误，直接标记失败，避免无限重试坏请求。

## 事件

| 事件 | 方向 | 幂等键 |
|---|---|---|
| `ProductSubmitted.v1` | secondhand 本地 Outbox；catalog/risk 消费端尚未接入 | eventId |
| `SecondhandOrderRequested.v1` | secondhand 本地恢复审计 | tradeType + tradeId |
| `SecondhandTradeSettled.v1` | secondhand → order/messaging | business key |
| `NotificationRequested.v1` | secondhand → messaging | dedupeKey |
| `OrderStatusChanged.v1` | order → secondhand | eventId |

通知投递失败不回滚商品成交。业务事务只写本地 Outbox；内置发布器只投递
`NotificationRequested.v1` 和 `SecondhandTradeSettled.v1` 到 `http://messaging:8084/internal/events`，
补齐统一 `EventEnvelope`、收件人快照和幂等头，成功后才标记 `PUBLISHED`，失败按有界指数退避重试。
其余 `NEW` 事件仍是本地审计事实，在 catalog/risk 消费端实现前不得宣称已送达。消费端必须按
`eventId` 或业务幂等键去重。
