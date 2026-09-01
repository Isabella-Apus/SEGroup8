# 依赖故障自动恢复复验索引

本目录保存 `secondhand-service → order-service` 在真实 K3s 隔离命名空间中的修复前后证据。实验没有改写原 `segroup8` 命名空间。

## 结论

| 轮次 | 候选代码/制品 | 结果 | 关键证据 |
|---|---|---|---|
| 契约与快照修复后首次复验 | `f8f87e6f` | 课程最低故障处理通过；自动恢复失败 | 故障时 HTTP 202/`RETRY`、探针均 UP；订单恢复后 180 秒仍为 `RETRY`，人工重复请求才创建 1 单 |
| 数据库时钟修复后复验 | `d6eee99b9c178d1b5a5cb7c4e11655c960dd8f7b` | 课程要求和增强自动恢复均通过 | 后台自动推进到 `CREATED`；重复请求后仍只有 1 单；地址快照完整入库 |

首次复验的根因不是缺少外部中间件。应用用 Asia/Shanghai 的 `LocalDateTime.now()` 写 `next_retry_at`，而 MySQL 用 UTC 的 `CURRENT_TIMESTAMP` 判断是否到期，导致重试记录在数据库看来要约 8 小时后才到期。修复后，写入和筛选都使用数据库时钟：`TIMESTAMPADD(SECOND, delay, CURRENT_TIMESTAMP)`。

成功轮次的恢复时间线为 `10:40:50 RETRY → 10:40:59 CREATED`；服务日志在 `10:40:57` 记录 `order linked` 和 `created=1`。最终请求尝试次数为 8，订单业务键为 `SECONDHAND:DIRECT_BUY:900500-v1`，数据库中匹配订单数为 1。

## 目录

- `environment/`：隔离环境的 Kubernetes 资源、事件和构件清单。
- `dependency-fault-recovery-success/`：名称是当时的目标名，实际结果为 `automaticRecoveryPassed=false`；保留用于失败根因对照。
- `dependency-fault-database-clock-success/`：数据库单一时钟修复后的正式通过轮次。
- `sha256-manifest.txt`：从服务器下载前生成的 53 个原始证据文件校验值；本索引为下载后补充，不在该清单内。

## 验收入口

- 机器结论：`dependency-fault-database-clock-success/summary.json`
- 故障期间响应：`02-buy-during-outage-response.json`
- 恢复时间线：`09-recovery-timeline.txt`
- 二手请求最终状态：`10-recovered-secondhand-state.tsv`
- 订单与地址快照：`11-recovered-order-state.tsv`
- 自动恢复日志：`14-secondhand-recovery.log`
- 候选提交与 JAR SHA-256：`00-run-state.env`

所有目录均不含 SSH、JWT、数据库或内部服务令牌。
