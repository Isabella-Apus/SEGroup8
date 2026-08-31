# MS-04 订单依赖故障注入报告

## 自动化回归

`OrderFailureRecoveryIntegrationTest` 的 2 个场景全部通过：

1. order 创建超时后，本服务返回 `RETRY`，保留商品冻结和持久化请求；恢复时沿用同一个
   `orderBusinessKey`，最终变为 `CREATED`，不生成第二条成交请求。
2. order 持续离线并达到重试上限后，请求变为 `FAILED`，商品从 `TRADE_PENDING` 原子恢复为
   `ON_SHELF`，并写入一条失败 outbox 事件。

订单状态事件也以 `eventId` 做幂等：重复投递返回 `DUPLICATE`，状态和 outbox 不重复变化。

## 真实进程级依赖演练

2026-08-30 在隔离 Docker Compose 环境运行真实 `secondhand-service`、MySQL 8.4.6 和独立 HTTP
订单契约进程，执行结果为 `PASSED`：

| 阶段 | 结果 |
|---|---|
| 依赖停止 | API 返回 `RETRY`，商品保持 `TRADE_PENDING`，readiness 仍为 `UP` |
| 依赖恢复 | 同一 business key 恢复为 `CREATED`，orderId 9001，请求记录仍只有 1 条，商品为 `SOLD` |
| 重试耗尽 | 尝试次数 2，状态 `FAILED`，请求记录 1 条，失败 outbox 1 条，商品恢复 `ON_SHELF` |

证据位于 `evidence/fault-drill/20260830-230030-summary.json`，并保留 API 响应、Compose 日志和订单契约进程日志。

## 结论边界

该实验已验证真实网络连接、真实服务进程和真实数据库下的受控降级与恢复，不只是 mock 单元测试。全队
`order-service` 尚未作为可独立启动依赖接入本分支，因此“停止全队真实 order-service”仍属于共享集成阶段，
不能用契约进程结果冒充。
