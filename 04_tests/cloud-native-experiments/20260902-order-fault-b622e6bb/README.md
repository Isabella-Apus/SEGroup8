# Order 依赖故障与自动恢复正式实验报告

## 1. 实验目标与隔离方式

本实验选择 `secondhand-service -> order-service` 作为典型依赖链路，主动将隔离命名空间中的 Order 从 1 个副本缩为 0，验证受影响请求得到设计好的可恢复结果、二手服务不随依赖一起崩溃，并在 Order 恢复后自动补建且不产生重复订单。

实验提交为 `b622e6bbb0447d6823b50e7789e4777f7131eb9b`。故障命名空间是 `segroup8-cloud-exp-20260902-fault-b622e6bb`；生产命名空间 `segroup8` 只接受健康观测，没有停止或修改生产 Order。

## 2. 故障期与恢复结果

| 检查项 | 结果 |
|---|---|
| 故障注入 | 隔离 Order `1 -> 0` |
| 二手购买响应 | HTTP 202，`requestStatus=RETRY` |
| 二手 liveness/readiness | `UP` / `UP` |
| 无关二手列表 | 正常返回 |
| Order 恢复 | 后台任务自动重试 |
| 最终请求状态 | `CREATED` |
| 重复请求 | HTTP 200，复用原结果 |
| 匹配订单数量 | 1 |
| 地址快照 | Identity 预检 HTTP 200，Order 已保存完整快照 |

故障期的“查询失败”被视为结果不确定，二手服务保持商品冻结和 `RETRY`，不会把网络失败误判为业务失败；恢复任务复用 `tradeType:tradeId` 业务键查询或创建，因此重复提交后仍恰好一单。

## 3. Order 停机对其他服务的影响

“服务正常”与“所有业务正常”必须分开判断：

| 服务 | 进程/探针 | Order 停机后的业务影响 |
|---|---|---|
| identity-governance | 正常 | 无直接依赖，身份、用户、地址和治理能力正常 |
| catalog-shop | 正常 | 商品、店铺、分类和搜索正常；完整新建单无法完成，发往 Order 的库存事件等待重试 |
| benefits-finance | 正常 | 钱包、优惠券和已接收请求可处理；不再收到 Order 发起的新支付、退款和结算请求 |
| messaging | 正常 | 聊天、已有通知和 WebSocket 正常；不会产生新的 Order 状态事件通知 |
| secondhand | 正常但部分降级 | 浏览、发布和议价正常；直购与拍卖结算进入 `RETRY`，恢复后补建单 |
| frontend | 可访问 | 非订单页面可用，订单相关路由和业务请求失败或等待恢复 |

本次正式实验只暂停隔离 Order。故障窗口观测到生产 backend、catalog-shop、identity、order、secondhand、messaging、finance 和 frontend 健康入口全部返回 HTTP 200，生产 Order endpoints 仍存在。

## 4. 原始证据

- `summary.json`：故障响应、探针、恢复、幂等和地址快照结论；
- `00-run-metadata.env`：实验提交及三个候选 JAR 的 SHA-256；
- `01-fault-injection.txt`、`08-recovery.txt`：Order 缩容和恢复；
- `02-*`、`06-*`、`09-*`、`10-*`、`11-*`、`12-*`、`13-*`：故障响应、状态时间线、恢复数据和重复请求；
- `03-*`、`04-*`、`05-*`：故障期间二手服务的探针和无关能力；
- `01a-production-namespace-isolation.txt`、`01a-*-health.txt`：生产命名空间隔离证明；
- `07-*`、`14-*`、`15-*`、`17-events.txt`：脱敏日志和 Kubernetes 事件；
- `environment/`：实验镜像、资源清单与候选制品元数据。

## 5. 复现

```bash
GIT_COMMIT=<commit> \
bash scripts/experiments/cloud-native/prepare_environment.sh <run-id> <host-root>

bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh \
  <host-root>/state.env dependency-fault-order

bash scripts/experiments/cloud-native/cleanup_environment.sh \
  <host-root>/state.env
```

现场应展示 HTTP 202/`RETRY`、探针保持 `UP`、Order 恢复后的 `CREATED`，以及重复调用后订单数量仍为 1。
