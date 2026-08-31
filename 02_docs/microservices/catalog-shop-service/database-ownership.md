# 数据归属与授权

| 表 | 模块 | 写入者 |
|---|---|---|
| `category`、`product`、`inventory_reservation`、`inventory_reservation_item` | catalog | 本服务 catalog 模块 |
| `shop` | shop | 本服务 shop 模块 |
| `product_risk_audit` | risk | 本服务 risk 模块 |
| `browse_history`、`user_search_history`、`search_keyword_stat` | behavior | 本服务 behavior 模块 |
| `idempotency_record`、`outbox_event` | technical | 本服务内部模块 |

生产授权应为 `GRANT SELECT,INSERT,UPDATE,DELETE ON catalog_shop_db.* TO catalog_shop_app`，不得授予全局权限。验收命令：以 `catalog_shop_app` 执行 `SELECT 1 FROM identity_governance_db.user LIMIT 1`，预期 MySQL `ERROR 1142`；再执行 `SELECT COUNT(*) FROM catalog_shop_db.product`，预期成功。原始输出保存至测试证据目录。
