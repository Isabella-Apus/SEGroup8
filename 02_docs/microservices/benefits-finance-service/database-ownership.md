# 数据库归属与权限隔离

所有表均由 Flyway `V1__benefits_finance_schema.sql` 创建，只允许
`benefits-finance-service` 写入。其他服务通过已登记的 `/internal/**` API 或 Outbox
事件获取数据，禁止直连本 Schema、跨库 join 或共享 Mapper/Entity。

| 表 | 主键 | 敏感/审计字段 | 关键约束与外部读取方式 |
|---|---|---|---|
| `voucher` | `id` | 发行人 `issuer_user_id`、适用店铺/商品 | 本服务独占读写；公开券 API 返回必要投影，库存和版本在本地 CAS |
| `user_voucher` | `id` | `user_id`、`order_request_id`、`used_order_id` | `(user_id,voucher_id)` 与 `order_request_id` 唯一；订单仅通过券预占/核销/释放 API 访问 |
| `balance` | `user_id` | 个人/经营余额 | 非负约束；钱包公开 API 和资金内部 API 返回投影，不向其他服务开放 SQL |
| `transaction_record` | `id` | 用户、订单、金额、反向流水 ID | 业务上仅追加；`transaction_id`、`business_request_id` 唯一；用户/卖家只能查询自己的流水 |
| `checkout_quote` | `quote_id` | 用户、订单请求、金额和券 | `order_request_id` 唯一、带版本与有效期；仅由 `/internal/checkout/quote` 返回 |
| `payment_request` | `request_id` | 用户/卖家、订单、金额、原支付请求 | 结算按 `(request_type,order_id,seller_id)` 唯一；订单用请求 ID 查询结果 |
| `idempotency_record` | `(scope,request_key)` | 响应快照 | 本服务内部幂等实现，不对外读取 |
| `outbox_event` | `event_id` | 业务 payload | 有界重试后进入 `DEAD`；消费者只通过事件端点接收，不查询该表 |

`shopId`、`productId`、`orderId`、`userId` 不建立跨 Schema 外键。应用账号建议只授予：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE ON benefits_finance_db.* TO 'benefits_finance_app'@'%';
REVOKE ALL PRIVILEGES ON order_db.* FROM 'benefits_finance_app'@'%';
```

验收时以应用账号执行 `UPDATE order_db.order_info ...`，预期为权限拒绝。DDL 由独立 Flyway 迁移账号执行，运行时账号不授予 `CREATE USER`、`GRANT` 或跨库权限。

测试证据由 `MySqlContractIntegrationTest` 保存：空库迁移、表归属、应用账号跨
Schema 权限拒绝、并发结算与并发退款均在 MySQL 8.4.6 上执行；Docker/Testcontainers
不可用时测试必须失败，不能跳过。
