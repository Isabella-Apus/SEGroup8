# 数据库归属

| 表 | 权限 | 用途 |
|---|---|---|
| `order_info` | order_db 独占读写 | 主表、履约/支付/退款状态与收货快照 |
| `order_item` | order_db 独占读写 | 商品、价格、店铺/卖家快照 |
| `order_after_sale_log` | order_db 独占读写 | 售后决定与仲裁轨迹 |
| `review` | order_db 独占读写 | 评价、追评、回复 |
| `logistics_path_template` / `logistics_trace` | order_db 独占读写 | 路径模板和轨迹 |
| `idempotency_record` | order_db 独占读写 | 请求哈希与稳定结果 |
| `order_saga` / `outbox_event` | order_db 独占读写 | 补偿、恢复和可靠事件 |

生产账号仅授予 `order_db.*`。`CrossSchemaPermissionTest` 使用真实 MySQL 创建 `benefits_finance_db.balance`，验证 `order_app` 查询被拒绝；没有 Docker 时跳过而不是用 H2 伪造权限结果。
