# 迁移版本报告

## 对比版本

| 阶段 | 可复核版本 | 说明 |
|---|---|---|
| 改造前 | tag `monolith-start`，commit `2d39751cbda8d4e6d6b4a10565a9f9f9e266f119` | 保留单体二手 Controller/Service 及直接写订单表的实现，作为回归基线 |
| 改造后 | branch `feature/ms-secondhand`，目录 `microservices/secondhand-service/` | 服务只拥有 `secondhand_db`；PR/merge SHA、镜像 digest 和 Helm revision 在合并、部署后补入 `delivery-manifest.md` |

## 跨领域依赖改造证据

| 业务路径 | `monolith-start` 中的直接耦合 | `secondhand-service` 中的替代实现 |
|---|---|---|
| UC17 直接购买 | `SecondhandProductServiceImpl#buySecondhandProduct` 注入 `OrderInfoMapper`、`OrderItemMapper`，直接执行 `orderInfoMapper.insert(...)` 和 `orderItemMapper.insert(...)` | `TradeApplicationService#buy` 只冻结本地商品并保存 `trade_order_request`；`TradeOrderCoordinator` 通过 `OrderGateway` 创建订单 |
| UC18 议价成交 | `SecondhandTradeServiceImpl#createPendingBargainOrder` 直接写订单和订单项 | 议价 CAS 后生成唯一 business key；`HttpOrderGateway` 调用 `POST /internal/orders/secondhand`，失败时按 business key 查询并恢复 |
| UC19 拍卖结算 | `SecondhandTradeServiceImpl#settleOneAuction` 直接写订单和订单项 | `TradeApplicationService#settleAuction` 原子确定赢家并生成幂等请求；定时任务可重入，订单创建仍统一交给 `TradeOrderCoordinator` |
| UC20 订单状态 | 单体可直接访问同库订单状态 | `InternalEventController` 消费 `OrderStatusChanged.v1`；`OrderStatusProjectionService` 按 `eventId` 幂等更新本地投影，不读取 `order_info` |
| 风险与通知 | 单体在进程内直接调用风险、聊天和通知服务 | 发布 `ProductSubmitted.v1`、`NotificationRequested.v1` 等 outbox 事件；风险决定通过内部事件入口回传 |

改造后的 Java 源码中不存在 `OrderMapper`、`OrderInfoMapper`、`OrderItemMapper`、`BalanceMapper`、`VoucherMapper` 或 `NotificationMapper`。订单服务 HTTP 契约集中在 `client/OrderGateway.java` 和 `client/HttpOrderGateway.java`，领域服务不能绕过该端口写订单库。`MySqlSchemaOwnershipIntegrationTest` 还使用受限账号验证：可以写本服务七张表，但向 `order_db.order_info` 插入必须失败。

复核命令：

```powershell
git grep -n -E "OrderInfoMapper|OrderItemMapper|orderInfoMapper\.insert|orderItemMapper\.insert" monolith-start -- backend/src/main/java/com/segroup8/platform/service/impl/SecondhandProductServiceImpl.java backend/src/main/java/com/segroup8/platform/service/impl/SecondhandTradeServiceImpl.java
rg -n "OrderMapper|OrderInfoMapper|OrderItemMapper|BalanceMapper|VoucherMapper|NotificationMapper" microservices/secondhand-service/src/main/java
rg -n "POST /internal/orders/secondhand|OrderStatusChanged.v1|NotificationRequested.v1" 02_docs/microservices/secondhand-service microservices/secondhand-service/src/main/java
```

第二条命令预期无输出；第一条展示基线中的直接 Mapper 依赖，第三条展示改造后的 API/事件契约。单体源码作为 `monolith-start` 对照版本继续保留，删除的是新微服务中的跨领域依赖，不应为了展示差异而改写基线。

## 数据迁移

| 版本 | 文件 | 内容 | 回滚策略 |
|---|---|---|---|
| V1 | `V1__secondhand_owned_schema.sql` | 7 张业务/技术表和本地分类投影 | 新服务首次部署可删除独立 schema；已写入业务数据后采用前向修复迁移 |
| V2 | `V2__auction_leading_bid_index.sql` | 为领先出价查询增加确定性索引，配合按主键更新避免不同拍卖间范围锁死锁 | 不回退已生效索引；如需调整，以新 Flyway 版本前向迁移 |

Flyway 在启动时执行并校验校验和。Kubernetes readiness 只在应用启动及 migration 成功后可用。

从单体迁移数据时应停写二手入口、导出四张原始二手表、转换状态并导入本 schema；订单 ID 只作为外部引用迁移。禁止把 `order_info`、余额、优惠券或通知表复制到本服务。
