# 跨服务调用与恢复

## 创建二手订单

Secondhand 调用 `POST http://segroup8-order:8085/internal/orders/secondhand`，携带
`X-Internal-Service-Token` 和稳定的 `Idempotency-Key`。请求与 Order 服务的
`SecondhandOrderRequest` 完全一致：

```json
{
  "tradeType": "DIRECT_BUY|BARGAIN|AUCTION",
  "tradeId": "业务内唯一值",
  "buyerUserId": 20,
  "sellerUserId": 10,
  "productId": 1,
  "productName": "Secondhand product #1",
  "price": 75.00,
  "receiverName": "Buyer",
  "receiverPhone": "00000000000",
  "receiverProvince": "PENDING",
  "receiverCity": "PENDING",
  "receiverDetailAddress": "address-id:3",
  "remark": "可选"
}
```

响应是 Order 服务实际返回的原始 `OrderView`，Secondhand 读取 `id`、`orderNo`
和 `orderStatus`。Order 以 `tradeType + ':' + tradeId` 作为唯一业务键；重复请求
返回同一订单。当前接口保留地址 ID 的可追踪占位快照，最终发货前应由订单域通过
用户地址契约固化完整脱敏快照，不能跨库读取地址表。

## 超时与补偿顺序

1. 本地事务将商品从 `ON_SHELF` CAS 为 `TRADE_PENDING`，并写入 `trade_order_request`。
2. 调用订单创建接口。
3. 调用异常时，先执行 `GET /internal/orders/by-business-key/{key}`，处理“订单已创建但响应丢失”。
4. 查询无结果才将请求置为 `RETRY`，恢复任务使用同一 business key 重试。
5. 达到 `ORDER_MAX_ATTEMPTS` 后置为 `FAILED` 并解除商品冻结。

## 事件

| 事件 | 方向 | 幂等键 |
|---|---|---|
| `ProductSubmitted.v1` | secondhand → catalog/risk | eventId |
| `SecondhandOrderRequested.v1` | secondhand outbox | tradeType + tradeId |
| `SecondhandTradeSettled.v1` | secondhand → order/messaging | business key |
| `NotificationRequested.v1` | secondhand → messaging | dedupeKey |
| `OrderStatusChanged.v1` | order → secondhand | eventId |

通知投递失败不回滚商品成交。readiness 只检查本地数据库与迁移，不把下游可用性
作为自身就绪条件。业务事务仅写本地 Outbox；共享 relay/CDC 接入前，`NEW` 记录
表示待投递，不能宣称已送达。消费端必须按 `eventId` 或表中业务幂等键去重。
