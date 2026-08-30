# 跨服务调用与恢复

## 创建二手订单

`POST /internal/orders/secondhand` 请求至少包含：

```json
{
  "tradeType": "DIRECT_BUY|BARGAIN|AUCTION",
  "tradeId": "业务内唯一值",
  "orderBusinessKey": "SECONDHAND:<tradeType>:<tradeId>",
  "productId": 1,
  "buyerId": 20,
  "sellerId": 10,
  "price": 75.00,
  "addressId": 3,
  "remark": "可选"
}
```

order-service 必须对 `tradeType + tradeId` 和 `orderBusinessKey` 唯一。正常响应中的初始订单状态为 `PENDING_PAY`。

## 超时与补偿顺序

1. 本地事务将商品从 `ON_SHELF` CAS 为 `TRADE_PENDING`，同时写 `trade_order_request`。
2. 调用订单创建接口。
3. 调用异常时先执行 `GET /internal/orders/by-business-key/{key}`，防止“响应丢失但订单已创建”造成重复订单。
4. 查询无结果才把请求置为 `RETRY`；恢复任务按相同 business key 重试。
5. 达到 `ORDER_MAX_ATTEMPTS` 后置为 `FAILED` 并解除商品冻结。

## 事件

| 事件 | 方向 | 幂等键 |
|---|---|---|
| `ProductSubmitted.v1` | secondhand → catalog/risk | eventId |
| `SecondhandOrderRequested.v1` | secondhand outbox | tradeType + tradeId |
| `SecondhandTradeSettled.v1` | secondhand → order/messaging | business key |
| `NotificationRequested.v1` | secondhand → messaging | dedupeKey |
| `OrderStatusChanged.v1` | order → secondhand | eventId |

通知事件失败不回滚商品成交。readiness 只检查本地数据库和 migration，不依赖 order-service。

本服务只负责在业务事务内写入 `outbox_event`。事件的实际发送由全队统一的 relay/CDC 组件读取
`event_status=NEW` 的记录并投递；在共享投递组件接入前，`NEW` 记录积压是待集成状态，不能据此宣称
消息已经送达下游。消费端继续以 `eventId` 或表中列出的业务幂等键去重。
