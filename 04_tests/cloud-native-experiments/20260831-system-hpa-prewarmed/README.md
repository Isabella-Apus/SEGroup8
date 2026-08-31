# 完整系统 HPA 最终复验（2026-08-31）

## 验收结论

本轮在云服务器现有 `segroup8` 完整系统上执行，复用已经发布的前端、后端和 MySQL
镜像及 Kubernetes 配置。所有压测请求都经过：

`Traefik -> frontend Nginx -> segroup8-backend -> MySQL`

HPA 绑定主要无状态计算层 `deployment/segroup8-backend`。这并不表示只测试一个
微服务：前端路由、完整后端与真实数据库均在请求链路中；MySQL 有状态层不参与水平扩容。

最终状态为 **PASS**：

- CPU 目标 60%，`minReplicas=2`、`maxReplicas=4`；
- 实际观察到 `2 -> 4（4 个均 Ready）-> 2`；
- 固定 2 副本与 HPA 两组共 6 个计量窗口，错误率全部为 0%；
- 扩容触发阶段持续 120 秒，新增 Pod 全部就绪后又完成一次业务预热才开始计量；
- 所有 Pod 重启数为 0；实验结束后临时数据为 0 条，复合索引保留；
- 服务器最终保留优化后的 HPA，空闲时为 2 个后端副本。

## 请求结果

| 模式 | 并发 | 吞吐（req/s） | 平均延迟（ms） | P95（ms） | 错误率 |
|---|---:|---:|---:|---:|---:|
| 固定 2 副本 | 5 | 28.41 | 175.78 | 428.74 | 0% |
| 固定 2 副本 | 10 | 30.05 | 329.89 | 873.23 | 0% |
| 固定 2 副本 | 20 | 30.19 | 655.67 | 1941.81 | 0% |
| HPA 2..4 | 5 | 13.78 | 362.11 | 1073.75 | 0% |
| HPA 2..4 | 10 | 18.83 | 527.54 | 1367.20 | 0% |
| HPA 2..4 | 20 | 21.75 | 908.35 | 2797.84 | 0% |

HPA 在该 4 vCPU 单节点上没有超过固定 2 副本吞吐。原因是扩到 4 个 Pod 并没有增加
物理 CPU，反而增加 JVM、连接池与 MySQL 的共享资源竞争。预热仍然有效：同为并发 20，
未做扩容后预热的调参轮次为 9.26 req/s，最终预热轮次提高到 21.75 req/s。该结果说明
HPA 的扩缩容和可用性门禁通过，但不能声称单节点“副本越多性能越高”；若要证明水平扩容
带来容量增长，应在多节点集群复测。

## 慢查询优化

5,000 条临时数据按在售/下架混合分布，保证 `status=1` 具有真实选择性。相同查询：

```sql
SELECT id, seller_user_id, name, sale_price, status, create_time
FROM secondhand_product
WHERE status = 1
ORDER BY create_time DESC
LIMIT 100;
```

优化前为全表扫描加排序，执行 2.71 ms；增加
`idx_secondhand_status_created(status, create_time DESC, id)` 后实际走索引，执行
0.395 ms，下降 85.4%。脚本会用 `INVISIBLE/VISIBLE` 重放两种执行计划，并在优化后
没有使用该索引时直接失败。

## 证据导航

- `summary.json`：固定/HPA 六个窗口与扩缩容门禁；
- `replica-timeline.csv`：逐时间点副本、Ready 副本和 CPU；
- `resource-snapshots.log`：Pod/节点资源采样；
- `database/summary.json`、`explain-before.txt`、`explain-after.txt`：SQL 优化；
- `database/post-cleanup-and-index.txt`：临时数据清理与索引保留；
- `environment/inventory.txt`、`routing.yaml`：服务器、镜像和完整请求链路；
- `environment/backend-liveness.json`、`backend-readiness.json`、`backend-version.json`：
  存活、就绪与版本；
- `environment/fresh-schema-validation.txt`：空数据库完整 schema、33 张表与复合索引字段校验；
- `environment/helm-hpa-adoption-dry-run.txt`：旧 release values 合并及现有 HPA 被 Helm 接管的
  server-side dry-run；
- `environment/final-state.txt`、`hpa-describe.txt`、`events.txt`：最终集群和 HPA 事件；
- `logs/`：前端、后端和 MySQL 日志；
- `sha256-manifest.txt`：精简证据文件校验值。

完整逐请求 raw JSON 仍保存在服务器
`/root/segroup8-experiments/system-hpa-20260831-system-hpa-prewarmed/raw/`，不提交 Git，
避免长期保存大体积重复原始记录。仓库内保留可审计摘要、时间线、资源、日志和关键执行计划。
