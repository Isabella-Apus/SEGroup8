# Cloud-native experiment runners

本目录包含两类用途不同的脚本。

## 完整系统 HPA 实验（当前验收口径）

助教确认 HPA 实验面向完整系统。脚本复用已部署的镜像、Secret、PVC、Service、Ingress
和 MySQL，不重建平行应用。压测流量始终经过真实公开链路：

`Traefik -> frontend Nginx -> segroup8-backend -> MySQL`

HPA 只能绑定一个 Kubernetes 可伸缩对象，不能直接绑定“整套系统”。因此它绑定主要无状态
计算层 `deployment/segroup8-backend`；前端和数据库仍参与每一次请求，但数据库不做水平扩容。

正式复测（固定 2 副本与 HPA 2..4 使用相同分阶段负载）：

```bash
RUN_ID=formal-$(date +%Y%m%d-%H%M%S) \
CONCURRENCIES='2 5 10' STAGE_DURATION=45 WARMUP_DURATION=30 \
bash scripts/experiments/cloud-native/run_system_hpa_experiment.sh
```

答辩现场一键演示（较短，仍保留预热、固定基线、扩容、缩容和证据）：

```bash
bash scripts/experiments/cloud-native/reproduce_system_hpa_demo.sh
```

两个脚本默认：

- 使用 `segroup8` 命名空间和 `http://127.0.0.1` 公开入口；
- 生成 5,000 条保留 ID 段的临时只读压测数据，结束时删除；
- 用索引 `INVISIBLE/VISIBLE` 重放同一条 SQL 的 `EXPLAIN ANALYZE`，索引本身保留；
- 先将后端固定为 2 副本并预热，再应用 CPU 60%、`minReplicas=2`、`maxReplicas=4` 的 HPA；
- 持续加载直到扩出的 Pod 全部就绪，再做一次业务预热后才开始计量，避免把 JVM 冷启动
  误判为 HPA 性能；
- 记录镜像、路由、请求、逐接口延迟、Pod/节点资源、HPA 时间线、事件和三层日志；
- 使用真实页面常用的 20 条/页负载和显式 15 秒超时；只有观测到扩容、回落到 2
  且最大错误率不超过 5% 才返回 0；
- 生成的 HPA 带有当前 Helm release 的归属元数据（默认 release 为 `segroup8`），后续启用
  chart 中的 `backend.autoscaling.enabled` 时可由同一个 Helm release 平滑接管；
- 成功后保留优化后的 HPA；设置 `KEEP_OPTIMIZED_HPA=false` 可恢复运行前状态；失败时总会恢复。

证据默认位于 `/root/segroup8-experiments/system-hpa-<RUN_ID>/`。答辩时重点展示
`summary.json`、`replica-timeline.csv`、`database/explain-before.txt`、
`database/explain-after.txt`、`resource-snapshots.log` 和 `hpa-describe.txt`。

## 隔离的微服务/依赖故障/性能实验（历史与其他课程实验）

以下脚本只创建名称匹配 `segroup8-cloud-exp-*` 的隔离命名空间：

1. `prepare_environment.sh`
2. `run_performance_comparison.sh`
3. `run_hpa_experiment.sh`（历史二手微服务 HPA，不再作为完整系统 HPA 结论）
4. `run_dependency_fault_experiment.sh`
5. `cleanup_environment.sh`

隔离运行器需要 Bash、Python 3、kubectl、Metrics API、缓存的 Java/MySQL 镜像，以及
`<host-root>/jars/` 下的身份治理、订单和二手服务 JAR。生成的凭据只保存在 Kubernetes
Secret 和服务器端渲染清单中，不应提交到 Git。

依赖故障实验不会覆盖既有证据：

```bash
bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh \
  <host-root>/state.env dependency-fault-<candidate>
```

它只有在受控故障、自动恢复、无重复订单和收件地址快照全部通过时才返回 0；失败时仍保留
`summary.json` 和诊断材料。
