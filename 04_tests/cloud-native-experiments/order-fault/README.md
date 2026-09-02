# Order 依赖故障与自动恢复正式实验报告

## 1. 实验目标与隔离方式

本实验在独立命名空间 `segroup8-cloud-exp-defense-ready` 中部署六个微服务，仅将隔离环境的 `order-service` 从 1 个副本缩为 0。上方状态表展示同一隔离环境中的六个微服务；下方继续展示 `secondhand-service -> order-service` 同步故障链路和 `catalog-shop-service -> order-service` 异步事件链路。

实验不读取、不停止也不修改正式系统命名空间。故障发生时，上方 Order 状态随实际 Deployment 变为“故障已注入”，其余五个微服务继续显示实际探针与副本状态。

## 2. 最终实验结果

最终证据目录：`dependency-fault-six-services-final-20260902-213617`，脚本退出码为 0。

| 检查项 | 结果 |
|---|---|
| 故障注入 | 隔离 Order `1 -> 0` |
| 隔离环境六服务展示 | 6/6，故障期仅 Order 的期望副本为 0 |
| 其余五服务连续性检查 | 15/15 通过：每个服务的 liveness、readiness、业务接口均成功 |
| 二手购买响应 | HTTP 202，`requestStatus=RETRY` |
| Catalog 异步事件 | 故障期保留为 `PENDING`，恢复后变为 `SENT` |
| Order 恢复 | Deployment 恢复为 1 个副本，两个依赖链路自动补偿 |
| 最终二手请求状态 | `CREATED` |
| Order 对 Catalog 事件的消费 | Inbox 记录恰好 1 条 |
| 重复二手购买请求 | HTTP 200，匹配订单数仍为 1 |
| 地址快照 | Identity 预检 HTTP 200，Order 保存完整快照 |
| 课程故障处理要求 | 通过 |
| 自动恢复增强目标 | 通过 |

## 3. Order 停机对六个微服务的影响

“服务仍然运行”不等于“所有涉及 Order 的业务都不受影响”。本次实测结论如下：

| 服务 | 故障期状态 | 业务影响 |
|---|---|---|
| identity-governance | 正常 | 身份、用户和地址能力正常，无直接 Order 依赖 |
| catalog-shop | 正常、异步降级 | 分类等业务正常；发往 Order 的事件可靠保存在 Outbox，恢复后自动投递 |
| benefits-finance | 正常 | 钱包/财务查询正常；不会收到停机期间尚未由 Order 发出的新请求 |
| messaging | 正常 | 通知查询等能力正常；不会收到停机期间尚未由 Order 发出的新状态事件 |
| secondhand | 正常、同步降级 | 浏览能力正常；购买请求进入 `RETRY`，Order 恢复后自动补建订单 |
| order | 故障已注入 | 副本数为 0、接口不可用；恢复后消费待处理请求和事件 |

## 4. 第五步耗时说明

本次第五步总计 125 秒，其中 Order Deployment 冷启动与就绪用了 71 秒，两个依赖链路的自动恢复轮询用了 54 秒。此前看到约 80 秒并不是脚本卡死：这台服务器上 Order 的 Spring Boot 冷启动本身约需一分钟，随后重试任务还要等待下一次退避窗口。

脚本现在分别输出 `orderRolloutSeconds`、`recoveryPollingSeconds` 和 `dependencyRecoveryStepSeconds`，现场可以直接判断时间花在启动还是业务补偿。实验不会人为修改重试时间来制造快速通过。

## 5. 核心证据

- `summary.json`：全部断言和三个分段耗时；
- `00-six-microservices-before-fault.txt`：故障前六服务均为 1/1；
- `01-six-microservices-during-fault.txt`：故障期 Order 为 0，其余五服务为 1/1；
- `service-continuity/results.tsv`：其余五服务的 15 项连续性检查；
- `09-recovery-timeline.txt`：Secondhand、Catalog、Order Inbox 的自动恢复时间线；
- `11b-catalog-outbox-after-recovery.tsv`、`11c-order-catalog-inbox-after-recovery.tsv`：异步事件投递与恰好一次消费证据；
- 其余响应、数据库状态、脱敏日志和 Kubernetes 事件文件用于追溯细节。

## 6. 复现与现场展示

```bash
GIT_COMMIT=<commit> \
bash scripts/experiments/cloud-native/prepare_environment.sh <run-id> <host-root>

bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh \
  <host-root>/state.env dependency-fault-order

source <host-root>/state.env
watch -n 1 -c env NAMESPACE="$NAMESPACE" HOST_ROOT="$HOST_ROOT" \
  bash scripts/experiments/cloud-native/show_dependency_fault_dashboard.sh
```

展示时，上方六服务表应随隔离 Order 的 `1 -> 0 -> 1` 实时变化；下方链路应显示 Secondhand `RETRY -> CREATED`、Catalog `PENDING -> SENT` 和 Order Inbox `0 -> 1`。
