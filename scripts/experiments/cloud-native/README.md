# 云原生实验脚本

课程最终只保留两个正式实验入口：完整系统 HPA，以及隔离环境中的 Order 依赖故障。历史二手微服务 HPA 已移出项目，不再提供脚本、Helm 模板或流水线门禁。

## 完整系统 HPA

`run_system_hpa_experiment.sh` 将 HPA 绑定共享系统后端 `deployment/segroup8-backend`，不绑定 `secondhand-service`。Kubernetes HPA 必须指向一个具体可伸缩工作负载，因此“完整系统”在这里表示：

- 从 frontend Service/Nginx 进入共享兼容后端和 MySQL；
- 使用首页、商品、分类、搜索、二手列表组成的混合负载；
- 同时记录公开入口、前端、后端、数据库、节点资源与 HPA 时间线；
- 固定 2 副本与 HPA 2..4 使用相同负载，扩出的 Pod 必须全部 Ready；
- 只有扩容、缩回 2 和错误率不高于 5% 同时成立才返回 0。

正式复现：

```bash
RUN_ID=formal-$(date +%Y%m%d-%H%M%S) \
KEEP_OPTIMIZED_HPA=false \
CONCURRENCIES='5 10 20' STAGE_DURATION=30 WARMUP_DURATION=20 \
SCALE_TRIGGER_DURATION=120 \
bash scripts/experiments/cloud-native/run_system_hpa_experiment.sh
```

答辩短演示：

```bash
KEEP_OPTIMIZED_HPA=false \
bash scripts/experiments/cloud-native/reproduce_system_hpa_demo.sh
```

默认最小 2 副本是最终可用性配置，不要求扩容必须逐级经过 1、2、4。设置 `KEEP_OPTIMIZED_HPA=false` 会在实验结束后恢复原副本数和原 HPA 状态。

## Order 依赖故障

故障实验只允许创建或操作 `segroup8-cloud-exp-*` 命名空间：

1. `prepare_environment.sh` 创建隔离 MySQL、单体和全部六个微服务；
2. `run_dependency_fault_experiment.sh` 将隔离 Order 缩为 0；
3. 验证 Identity、Catalog、Finance、Messaging、Secondhand 的 liveness、readiness 和代表业务接口在故障窗口全部正常；
4. 验证二手购买返回 HTTP 202/`RETRY`，恢复后自动进入 `CREATED`、只生成一单并保存地址快照；
5. 验证 Catalog 发往 Order 的事件在 Outbox 中保留，Order 恢复后自动发送且 Order Inbox 恰好接收一次；
6. 同时观测生产命名空间全部服务与生产 Order endpoints，证明命名空间隔离；
7. `cleanup_environment.sh` 删除隔离命名空间并保留证据。

```bash
GIT_COMMIT=<commit> \
bash scripts/experiments/cloud-native/prepare_environment.sh <run-id> <host-root>

bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh \
  <host-root>/state.env dependency-fault-order

bash scripts/experiments/cloud-native/cleanup_environment.sh \
  <host-root>/state.env
```

六个实验 JAR 放在 `<host-root>/jars/`：`identity-governance-service-1.0.0.jar`、`catalog-shop-service-1.0.0.jar`、`order-service-1.0.0.jar`、`secondhand-service-1.0.0.jar`、`benefits-finance-service-1.0.0.jar`、`messaging-service-1.0.0.jar`。生成的 Secret、JWT、数据库密码和渲染清单不得提交；可提交 `summary.json`、探针响应、资源清单、事件、脱敏日志和数据库验证结果。

三类依赖采用不同断言：Secondhand 对 Order 是同步建单依赖，验证受控降级、自动恢复和重复请求不产生重复订单；Catalog 对 Order 是异步事件依赖，验证 Outbox 保留与恢复投递；Finance 和 Messaging 的调用方向均为 Order 指向它们，所以 Order 停止时验证其自身业务接口继续返回成功，不伪造不存在的补偿流程。
