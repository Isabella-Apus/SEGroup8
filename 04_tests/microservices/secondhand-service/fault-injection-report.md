# MS-04 订单依赖故障注入报告

执行用例：`OrderFailureRecoveryIntegrationTest`，结果 2/2 通过。

## 场景一：创建订单超时后恢复

1. 模拟 order-service 创建接口超时。
2. secondhand-service 返回 `RETRY` 和“订单创建处理中”，保留商品冻结与持久化请求。
3. 恢复阶段继续使用同一个 `orderBusinessKey` 查询/创建。
4. order-service 恢复后请求变为 `CREATED`，没有第二条成交请求。

## 场景二：达到重试上限

1. 模拟 order-service 持续离线。
2. 达到配置的最大次数后请求变为 `FAILED`。
3. 商品从 `TRADE_PENDING` 原子恢复为 `ON_SHELF`。
4. 写入 `SecondhandTradeOrderFailed.v1` outbox 事件，全程不写订单库。

## 事件重复投递

API 测试以同一个 `eventId` 两次投递订单取消事件，第一次返回 `CONSUMED`，第二次返回 `DUPLICATE`，状态观察 outbox 事件数量保持为 1。

当前证据为自动化故障注入。真实 Kubernetes 中停止 order-service、观察日志并恢复的演示仍需在可用集群执行，步骤已写入运维手册。
