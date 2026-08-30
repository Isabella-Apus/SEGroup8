# MS-04 API 与集成测试报告

执行时间：2026-08-29 21:37（Asia/Kuala_Lumpur）
执行命令：`mvn -B --no-transfer-progress -f microservices/pom.xml -pl secondhand-service -am clean verify`

## 结果

| 模块 | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `security-contract` | 5 | 0 | 0 | 0 |
| `secondhand-service` | 16 | 0 | 0 | 0 |
| 合计 | 21 | 0 | 0 | 0 |

## 覆盖内容

- UC16：公开查询、发布、风险待审、风险事件幂等、鉴权、分类/价格校验、非所有者编辑拒绝。
- UC17：匿名与自购拒绝、同一买家重复请求复用 business key、双买家并发只生成一个成交请求。
- UC18：申请、列表、有效议价语义、非所有者确认拒绝、确认成交、重复处理拒绝、同一议价只生成一个订单。
- UC19：创建、公开详情、卖家列表、自购式出价拒绝、低价拒绝、有效出价、所有权、成交和流拍；并发出价只有一个赢家，重复结算不重复建单。
- UC20 协作：内部 token 校验、订单取消事件消费、同一 `eventId` 返回 `DUPLICATE` 且只产生一条状态观察事件。
- 订单契约：`POST /internal/orders/secondhand` 携带 `tradeType + tradeId` 和幂等键；不确定失败先按 business key 查询。
- 数据边界：MySQL 8.4 中 Flyway 创建 7 张自有表，`secondhand_app` 可写自有表，但插入 `order_db.order_info` 被数据库拒绝。
- 架构边界：源码不含 `OrderMapper`、`BalanceMapper`、`VoucherMapper`、`NotificationMapper`，也不复制 `/api/order/**` 或 `/api/logistics/**`。

原始报告：`evidence/raw-reports/surefire/`。
