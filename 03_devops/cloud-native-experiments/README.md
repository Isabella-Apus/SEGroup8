# 云原生与性能实验交付报告

## 1. 结论与验收边界

本次实验在分支 `experiment/cloud-native-performance` 上完成。性能与依赖故障轮次编号为
`20260830-2300-ef71f1fe`，自动恢复修复复验编号为
`20260831-recovery-d6eee99b`；助教确认 HPA 应面向完整系统后，最终 HPA 复验编号为
`20260831-system-hpa-prewarmed`。实验使用一台 4 vCPU、7.1 GiB 内存的 K3s
v1.36.3 单节点服务器。性能与依赖故障仍在隔离命名空间中运行；完整系统 HPA 直接复用
`segroup8` 中已发布的前端、后端、MySQL 镜像和配置。HPA 临时数据已清零，最终保留
CPU 60%、`minReplicas=2`、`maxReplicas=4` 的后端 HPA。

验收状态：

| 项目 | 状态 | 结论 |
|---|---|---|
| 完整系统 HPA 扩缩容 | **通过** | 公开链路完整经过前端、后端和 MySQL；观察到 `2→4（4 Ready）→2`，6 个计量窗口均 0% 错误 |
| 依赖故障最低课程要求 | **通过** | 停止订单服务后，二手购买返回 HTTP 202/`RETRY`；二手服务存活、就绪和无关查询保持可用 |
| 依赖恢复增强目标 | **通过** | 保留首次 `RETRY` 失败证据；统一数据库时钟后，订单依赖恢复时后台自动推进 `CREATED`，恰好生成 1 单并保存地址快照 |
| 改造前后性能 | **完成** | 3 个公开接口、单体/微服务各 3 轮、相同数据和压力脚本；包含吞吐、平均、P95、错误率、CPU、内存 |

这里有意保留“课程最低要求先通过、增强恢复首次失败、修复后通过”的完整过程。不能因为健康检查为 UP
就声称业务恢复成功，也不能删除失败证据只保留最终成功结果。

## 2. 实验拓扑与公平性控制

```mermaid
flowchart LR
  L[同一 HTTP 压测器] --> M[改造前：单体后端\n1 Pod / 500m CPU]
  L --> S[改造后：secondhand-service\n1 Pod / 500m CPU]
  M --> MDB[(monolith_db\n500 条固定数据)]
  S --> SDB[(secondhand_db\n500 条同源数据)]
  S -->|内部 HTTP、超时、重试状态| O[order-service]
  O --> ODB[(order_db)]
```

公平性控制如下：

- 单体与微服务位于同一节点，数据库位于同一 MySQL 实例的不同 schema。
- 两边各插入 ID `900001..900500` 的 500 条同源二手商品，详情固定查询
  `900001`。
- 正式性能对比时删除 HPA，固定为单体 1 副本对微服务 1 副本；两边均为
  CPU request 100m / limit 500m、内存 request 256Mi / limit 768Mi。
- 使用同一无第三方依赖的 HTTP 压测器；并发 5、预热 3 秒、测量 20 秒；
  奇数轮先单体，偶数轮先微服务，降低时间顺序偏差。
- 每个窗口开始前同时检查 Deployment available 和 readiness；每 2 秒采集
  目标 Pod 的 CPU/内存。
- 正式资源采样峰值：单体 500m/247Mi，微服务 497m/234Mi；两边均实际触及
  相同 CPU 上限。

运行包没有因 Docker Hub 超时而重新构建一个来源不明的镜像。实验将已构建
JAR 挂载到服务器已缓存且固定摘要的 Java 基础镜像中，并记录双重身份：

| 构件 | 固定值 |
|---|---|
| 单体镜像标签 | `backend:sha-bb72290cff96c78ab189468b82db1f8ba3cd9323` |
| 单体镜像摘要 | `sha256:c15925877511353f870b7f811ba81af3a53dccac3acbfddddbf29b928a2a8f5a` |
| 二手 JAR SHA-256 | `52717f94e705d42e3668b994794205463ae9861725099656a7679fd196216e1e` |
| 订单 JAR SHA-256 | `4bcc3686fcd28432c8ef006298eb44f8d0a67b35d9fa7d862e81ea0e0e24909f` |
| 实验起始提交 | `ef71f1fe2349` |

该挂载方式只用于本次隔离实验，不替代三个微服务各自流水线生产的不可变候选镜像。

## 3. 完整系统 HPA 实验

### 3.1 口径与拓扑

Kubernetes HPA 必须绑定一个可伸缩对象，不能绑定“整个系统”这个抽象概念。本实验让
所有压力都经过真实公开入口，并把主要无状态计算层作为伸缩目标：

```mermaid
flowchart LR
  L[加权 HTTP 压测器] --> T[Traefik / 公网入口]
  T --> F[frontend Nginx]
  F --> B[segroup8-backend\nHPA 2..4]
  B --> DB[(MySQL / PVC)]
  MS[Metrics Server] --> H[HPA autoscaling/v2\nCPU 60%]
  H --> B
```

因此它不是“只压一个微服务”：前端路由、完整后端和真实数据库都参与请求；只有不保存
本地业务状态的后端 Pod 被水平扩缩，MySQL 不做错误的多副本水平扩容。

### 3.2 最终配置与结果

配置为 CPU 平均利用率 60%，最小 2、最大 4；扩容每 15 秒最多增加 2 个 Pod，
缩容稳定窗口 60 秒、每 15 秒最多减少 50%。脚本先测固定 2 副本基线，再施加 120 秒
扩容触发负载，等新增 Pod 全部 Ready 后做第二次业务预热，最后用相同接口比例分阶段计量。

| 模式 | 并发 | 吞吐（req/s） | 平均（ms） | P95（ms） | 错误率 |
|---|---:|---:|---:|---:|---:|
| 固定 2 副本 | 5 | 28.41 | 175.78 | 428.74 | 0% |
| 固定 2 副本 | 10 | 30.05 | 329.89 | 873.23 | 0% |
| 固定 2 副本 | 20 | 30.19 | 655.67 | 1941.81 | 0% |
| HPA 2..4 | 5 | 13.78 | 362.11 | 1073.75 | 0% |
| HPA 2..4 | 10 | 18.83 | 527.54 | 1367.20 | 0% |
| HPA 2..4 | 20 | 21.75 | 908.35 | 2797.84 | 0% |

最终时间线为 `2→4（4 个均 Ready）→2`，所有 Pod 重启数为 0，六个计量窗口全部
0% 错误。首轮 100 条/页负载虽完成扩缩容，但 `hpa-c5` 错误率 5.1546%，超过 5%
门禁并返回非零退出码；该失败证据保留在
`04_tests/cloud-native-experiments/20260831-system-hpa-failed-v1/`。

扩容后业务预热把并发 20 的 HPA 吞吐从未充分预热轮次的 9.26 提升到
21.75 req/s。最终 HPA 吞吐仍低于固定 2 副本，因为这是 4 vCPU 单节点：增加 Pod
不会增加物理算力，反而增加 JVM、连接池与 MySQL 的共享资源竞争。因此本轮证明了
扩缩容机制、Ready 容量和无错误服务，但不声称“单节点副本越多越快”。若课程要求证明
容量随节点增加，应另在多节点集群复测。

### 3.3 慢查询优化与可复现性

二手公开列表的代表查询在 5,000 条混合状态数据上，优化前为全表扫描加排序，
`EXPLAIN ANALYZE` 为 2.71 ms。新增
`idx_secondhand_status_created(status, create_time DESC, id)` 后实际走索引，降到
0.395 ms，下降 85.4%。脚本用索引 `INVISIBLE/VISIBLE` 重放前后计划；没有观察到
全表扫描或优化后未使用索引都会失败。

完整证据位于
`04_tests/cloud-native-experiments/20260831-system-hpa-prewarmed/`。一键复现命令为：

```bash
bash scripts/experiments/cloud-native/reproduce_system_hpa_demo.sh
```

脚本复用当前不可变镜像、Secret、PVC、Service 和 Ingress，自动生成并清理保留 ID 段的
临时数据，记录健康、就绪、版本、日志、事件、资源和副本时间线。成功后保留优化 HPA；
设置 `KEEP_OPTIMIZED_HPA=false` 可在演示后恢复运行前状态。

## 4. 依赖故障实验

故障链路选择 `secondhand-service → order-service`，因为它既能验证公开购买接口，
又能验证真实跨服务依赖，且订单不可用不应拖垮二手浏览。

实验过程：

1. 将隔离命名空间中的 `order-service` 从 1 副本缩到 0。
2. 使用合法买家 JWT 调用 `POST /api/secondhand/900500/buy`。
3. 得到 HTTP 202，业务状态为 `RETRY`；这是受控降级，不是 500 或连接异常透传。
4. 同期 `/actuator/health/liveness` 和 `/actuator/health/readiness` 均为 UP，
   无关二手列表接口仍返回业务数据。
5. 首次复验恢复订单服务到 1/1 后轮询 180 秒，请求始终停在 `RETRY`；人工重复同一购买请求才创建 1 单。
6. 查询 MySQL 时发现 `CURRENT_TIMESTAMP` 为 UTC 约 `02:16`，而 `next_retry_at` 被应用按
   Asia/Shanghai 写成约 `10:01`，记录要约 8 小时后才满足重试筛选条件。
7. 将重试时间写入改为 `TIMESTAMPADD(SECOND, delay, CURRENT_TIMESTAMP)`，让写入和到期判断使用同一个
   数据库时钟；重新构建候选 JAR 并保持其余实验条件不变。
8. 第二次复验中，故障期间仍返回 HTTP 202/`RETRY` 且探针均 UP；订单服务恢复后，时间线在 9 秒内从
   `RETRY` 进入 `CREATED`，无需人工重发。随后重复购买返回 200，数据库仍恰好 1 单。

因此，课程要求的“停止一个依赖、采用超时/受控返回、其他服务不崩”和增强目标
“依赖恢复后自动补偿且恰好生成一单”均已满足。最终订单包含身份治理服务返回的
`Experiment Buyer / 13800008000 / Zhejiang / Hangzhou / West Lake Road 1` 快照。

本实验共修复了两层真实问题。第一层是二手消费者与订单提供者契约不一致：请求补齐固定
`orderBusinessKey`、商品和收货地址快照，响应统一为 `{code,message,data}`，并增加 provider/consumer
契约测试。第二层是容器环境才暴露的跨时区双时钟问题：应用时钟写入、数据库时钟筛选导致
自动恢复延迟 8 小时。最终代码用数据库单一时钟计算重试到期点，且实验脚本在
`automaticRecoveryPassed=false` 时返回非零退出码，流水线不能再把失败实验显示为绿色。

### 课程范围

课程原文要求的是小组完成“两项云原生实验”，故障实验表述为主动停止“一个依赖服务”，
现场演示也要求停止一个依赖。因此不需要把每一对微服务依赖都完整停机演练一次。推荐验收口径是：

- 所有真实跨服务调用都必须有接口清单、数据归属、超时、鉴权、幂等/补偿策略和自动契约测试；
- 选择一条最能体现跨服务写链路的典型链路做完整故障注入、恢复、日志和数据库证据；
- 本系统最合适的典型链路就是 `secondhand-service → order-service`，因为可同时验证受控降级、
  重试、状态查询、幂等建单、地址快照和非相关浏览不被拖垮；
- 后续新服务只需通过统一基础治理清单。除非其故障模型不同（例如支付未知结果、消息队列积压），
  才增加专项实验，而不是机械重复本次演练。

## 5. 改造前后性能

正式结果采用三轮中位数；错误率按三轮总请求加权。单位为 req/s 和 ms。

| 接口 | 架构 | 吞吐 | 平均 | P95 | 错误率 |
|---|---|---:|---:|---:|---:|
| 普通列表 | 单体 | 1.00 | 2971.30* | 4263.64* | 55.13% |
| 普通列表 | 微服务 | 23.69 | 210.63 | 302.73 | 0% |
| 关键字+价格排序 | 单体 | 2.09 | 2355.80 | 2884.43 | 0% |
| 关键字+价格排序 | 微服务 | 47.64 | 104.86 | 185.21 | 0% |
| 详情 | 单体 | 27.29 | 183.08 | 301.28 | 0% |
| 详情 | 微服务 | 205.79 | 24.27 | 77.20 | 0% |

`*` 普通列表的单体前两轮为 100% 超时，平均/P95 只能来自第三轮成功样本，不能
视为三轮中位数。该接口最重要的结论是错误率差异，不是延迟倍率。

最有代表性的 4 个结果：

1. 详情吞吐从 27.29 提升到 205.79 req/s，约 **7.54 倍**。
2. 详情平均延迟从 183.08 降到 24.27 ms，下降约 **86.74%**。
3. 关键字查询吞吐从 2.09 提升到 47.64 req/s，约 **22.74 倍**；P95 从
   2884.43 降到 185.21 ms，下降约 **93.58%**。
4. 普通列表微服务三轮 0 错误；单体三轮加权错误率 55.13%。

这些结果只适用于本次代码、500 条数据、单节点和给定压力。不能外推为任意生产
规模下微服务都必然更快。探索性并发 20 时单体触发 3 次探针重启，已原样保存于
`performance-exploratory-overload-20/`，不混入正式表。

## 6. 一次部署失败是如何查出来的

第一次执行环境准备脚本时，日志只出现 containerd 的弃用警告，命名空间也没有创建。
先用 `kubectl get namespace` 证明失败发生在写入集群之前，再查看脚本停止位置，发现它
卡在“读取单体镜像摘要”的管道。脚本启用了 `set -euo pipefail`，其中 `awk` 找到目标
后提前 `exit`，导致仍在输出的 `ctr images list` 收到 SIGPIPE；`pipefail` 把这个正常的
提前结束当成整条管道失败。修复为让 `awk` 读到结尾再输出摘要后，第二次部署创建了
命名空间，MySQL、单体、订单、二手依次通过 rollout 和 readiness。两次日志都保留，
所以可以从“症状→失败边界→根因→修复→验证”完整复盘，而不是只写“重试后成功”。

## 7. 证据导航与复现

初始实验原始证据入口：`04_tests/cloud-native-experiments/20260830-2300-ef71f1fe/`。
自动恢复修复前后复验证据入口：
`04_tests/cloud-native-experiments/20260831-recovery-d6eee99b/`。完整系统 HPA 最终证据入口：
`04_tests/cloud-native-experiments/20260831-system-hpa-prewarmed/`；首轮门禁失败保留在
`04_tests/cloud-native-experiments/20260831-system-hpa-failed-v1/`。

| 目录 | 内容 |
|---|---|
| `environment/` | 服务器规格、Kubernetes 资源、构件身份、两次部署日志、数据条数 |
| `performance/raw/` | 正式 18 份逐请求延迟 JSON 与控制台摘要 |
| `performance/resources/` | 正式每 2 秒 CPU/内存采样 |
| `performance-exploratory-overload-20/` | 被明确降级为探索性失败的并发 20 结果 |
| `hpa/` | 历史二手微服务 HPA 调参材料，不再作为完整系统 HPA 验收结论 |
| `hpa-overload-60/` | 三轮过载边界实验 |
| `dependency-fault/` | 故障响应、健康检查、数据库状态、恢复时间线、两侧日志与事件 |

复验目录：

| 目录 | 内容 |
|---|---|
| `dependency-fault-recovery-success/` | 首次增强复验，实际 `automaticRecoveryPassed=false`；180 秒时间线、手工重发和根因证据 |
| `dependency-fault-database-clock-success/` | 数据库时钟修复后正式通过轮次；自动恢复、唯一订单、地址快照和调度日志 |
| `environment/` | 复验环境、Kubernetes 资源和制品元数据 |

完整系统 HPA 直接复用当前部署，无需重新上传三个微服务 JAR。答辩现场执行：

```bash
bash scripts/experiments/cloud-native/reproduce_system_hpa_demo.sh
```

性能对比和依赖故障仍使用隔离环境；在 K3s 节点准备好身份治理、订单、二手三个 JAR
并放到 `$HOST_ROOT/jars/` 后依次执行：

```bash
GIT_COMMIT=<commit> bash scripts/experiments/cloud-native/prepare_environment.sh <run-id> <host-root>
CONCURRENCY=5 DURATION=20 WARMUP=3 bash scripts/experiments/cloud-native/run_performance_comparison.sh <host-root>/state.env
bash scripts/experiments/cloud-native/run_dependency_fault_experiment.sh <host-root>/state.env <evidence-name>
bash scripts/experiments/cloud-native/cleanup_environment.sh <host-root>/state.env
```

生成的 Secret、渲染后清单和数据库密码不进入 Git；证据中也没有 SSH 密码、JWT 或
数据库密码。

## 8. 提交前验证

- `bash -n scripts/experiments/cloud-native/*.sh`：通过。
- `python -m py_compile scripts/experiments/cloud-native/http_benchmark.py scripts/experiments/cloud-native/http_mix_benchmark.py`：通过。
- `helm lint` 以及启用 `backend.autoscaling.enabled=true` 的 `helm template`：服务器通过，
  渲染值为 CPU 60%、2..4 副本。
- `mvn -Dtest=SecondhandProductServiceImplTest test`：6 个测试通过；本地全量测试曾因
  120 秒执行上限被终止，不将该次超时写成全量通过，完整回归由分支 CI 再验证。
- Windows 受限环境中的 Testcontainers 集成测试因 Docker named pipe `Access is denied` 未实际启动，记为
  `NOT_RUN`，不归类为代码失败。随后在云服务器创建一次性空数据库并执行完整 `schema.sql`：成功创建
  33 张表，`idx_secondhand_status_created` 的 3 个索引字段均存在，校验结束后临时库已删除。
- Helm 接管 dry-run 首先发现旧 release values 缺少后来新增的服务节点；四条发布脚本已统一从纯
  `--reuse-values` 改为 `--reset-then-reuse-values`，兼顾新 chart 默认值和各服务已发布值。
- 完整系统 HPA 精简证据 30 个文件的 SHA-256 复核通过；逐请求 raw JSON 留在服务器，
  不进入 Git。
- 164 个仓库内证据文件均写入 `sha256-manifest.txt`；所有 JSON 均可解析。
- `mvn -B --no-transfer-progress -f microservices/pom.xml -pl identity-governance-service,order-service,secondhand-service -am clean test`：
  非沙箱运行 BUILD SUCCESS；当前报告统计 security-contract 5、identity-governance-service 17、
  secondhand-service 22、order-service 14，共 58 个测试，0 failure、0 error、0 skipped。
- 修复候选提交 `d6eee99b9c178d1b5a5cb7c4e11655c960dd8f7b`；二手 JAR SHA-256 为
  `e096a85fcda1c846d18e8ef5cc85a58fc6f223d9967b9e42bbf3390fd9d96bf6`。
- 同一 Maven 命令在受限沙箱内曾因 Windows `Access is denied` 导致 testCompile
  看不到已生成 class；非沙箱重跑成功，故归类为执行环境问题，不归类为代码测试失败。
- 实验命名空间清理后，服务器只剩原 `segroup8` 业务命名空间；原后端、前端、
  MySQL 均 Running，首页和 `/api/secondhand/list` 均为 HTTP 200。
