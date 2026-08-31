# 服务边界、状态机与 Saga

## 边界

服务独占新品及二手订单、订单明细快照、售后轨迹、评价、物流轨迹、幂等记录、Saga 和 outbox。商品、地址、店铺、优惠券和余额只通过版本化契约取得快照或执行命令；代码中不含 `ProductMapper`、`SecondhandProductMapper`、`BalanceMapper`、`VoucherMapper`、`AddressMapper`、`NotificationMapper`。

公开请求校验 identity 签发的 JWT；内部接口校验 `X-Internal-Service-Token`。写接口要求 `Idempotency-Key`，支付和退款超时后按原 requestId 查询，不做无条件重试。

## 状态机

```mermaid
stateDiagram-v2
  [*] --> CREATING
  CREATING --> PENDING_PAY: 库存预留+订单提交
  PENDING_PAY --> PAYMENT_PENDING: 发起扣款
  PAYMENT_PENDING --> PENDING_PAY: 明确失败
  PAYMENT_PENDING --> PENDING_SHIP: 扣款成功
  PENDING_PAY --> CANCEL_PENDING: 取消
  PENDING_SHIP --> CANCEL_PENDING: 取消
  CANCEL_PENDING --> CANCELLED: 库存/券补偿完成
  PENDING_SHIP --> SHIPPED: 卖家发货
  SHIPPED --> RECEIVED: 买家确认收货
  RECEIVED --> COMPLETED: 评价或完成
  PENDING_SHIP --> REFUND_PENDING: 同意退款
  SHIPPED --> REFUND_PENDING: 同意退款
  RECEIVED --> REFUND_PENDING: 同意退款
  COMPLETED --> REFUND_PENDING: 同意退款
  REFUND_PENDING --> REFUNDED: 资金反向流水成功
```

退款申请先在 `refund_status` 进入 `REQUESTED`，卖家或管理员决定后进入 `REFUND_PENDING/REFUNDED` 或 `REJECTED`，不破坏履约状态的审计语义。

## Saga

```mermaid
sequenceDiagram
  participant C as Client
  participant O as order-service
  participant K as catalog
  participant F as finance
  participant X as order_db/outbox
  C->>O: create(Idempotency-Key)
  O->>K: reserve(reservationId)
  K-->>O: product snapshots
  O->>F: quote(quoteRequestId)
  F-->>O: payable snapshot
  O->>X: order + items + idempotency + outbox
  alt local commit fails
    O->>K: release(reservationId)
    opt release fails
      O->>X: compensation pending
    end
  end
  C->>O: pay(paymentRequestId)
  O->>F: debit once
  alt timeout
    O->>F: query by paymentRequestId
  end
  O->>X: paid state + outbox
  O->>K: confirm reservation
```
