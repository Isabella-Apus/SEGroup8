# 跨服务调用与恢复

## 创建二手订单

`POST /internal/orders/secondhand` 请求至少包含：

```json
{
  "tradeType": "DIRECT_BUY|BARGAIN|AUCTION",
  "tradeId": "业务内唯一值",
  "orderBusinessKey": "SECONDHAND:<tradeType>:<tradeId>",
  "productId": 1,
  "productName": "交易创建时的商品名称快照",
  "buyerUserId": 20,
  "sellerUserId": 10,
  "price": 75.00,
  "receiverName": "买家姓名",
  "receiverPhone": "13800000000",
  "receiverProvince": "浙江省",
  "receiverCity": "杭州市",
  "receiverDetailAddress": "详细地址",
  "remark": "可选"
}
```

地址 ID 不跨服务传给 order-service。secondhand-service 先调用
`GET /internal/users/{buyerId}/address-snapshot`，由 identity-governance-service 校验所有权，
再把不可变地址快照和商品名写入自己的 `trade_order_request`。order-service 必须对
`orderBusinessKey` 唯一，正常响应统一为 `{code,message,data:{orderId,orderNo,status}}`，
初始订单状态为 `PENDING_PAY`。

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
| `ProductSubmitted.v1` | secondhand → catalog/risk | eventId |
| `SecondhandOrderRequested.v1` | secondhand outbox | tradeType + tradeId |
| `SecondhandTradeSettled.v1` | secondhand → order/messaging | business key |
| `NotificationRequested.v1` | secondhand → messaging | dedupeKey |
| `OrderStatusChanged.v1` | order → secondhand | eventId |

通知事件失败不回滚商品成交。readiness 只检查本地数据库和 migration，不依赖 order-service。

本服务只负责在业务事务内写入 `outbox_event`。事件的实际发送由全队统一的 relay/CDC 组件读取
`event_status=NEW` 的记录并投递；在共享投递组件接入前，`NEW` 记录积压是待集成状态，不能据此宣称
消息已经送达下游。消费端继续以 `eventId` 或表中列出的业务幂等键去重。
