# 数据库归属与权限

## 独占表

| 表 | 用途 | 关键约束 |
|---|---|---|
| `secondhand_product` | 商品和交易冻结 | `status + version` CAS，售价不高于原价 |
| `product_negotiation` | 议价 | PENDING → ACCEPTING → ACCEPTED/FAILED |
| `product_auction` | 拍卖窗口和领先者 | `version` CAS；ONGOING → SETTLING → FINISHED |
| `auction_log` | 有效出价历史 | 一次成功 CAS 对应一条记录 |
| `trade_order_request` | 跨服务成交恢复 | 唯一 `(trade_type, trade_id)` 和 business key |
| `idempotency_record` | 消费事件和命令幂等 | 唯一 `(scope_name, idempotency_key)` |
| `outbox_event` | 可靠事件 | `event_id` 唯一，业务事务内写入 |
| `category_projection` | 本地只读分类投影 | 不访问 catalog 数据库 |

## 权限脚本

```sql
CREATE DATABASE IF NOT EXISTS secondhand_db CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'secondhand_app'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX
  ON secondhand_db.* TO 'secondhand_app'@'%';
REVOKE ALL PRIVILEGES ON order_db.* FROM 'secondhand_app'@'%';
FLUSH PRIVILEGES;
```

自动化 MySQL 测试先以管理员创建 `order_db.order_info`，再用 `secondhand_app` 执行 `INSERT` 并要求数据库拒绝。源码边界测试同时检查禁用 Mapper、订单表名和订单/物流 Controller 路径。
