# 身份治理、订单、二手服务课程交付对照

## 1. 核对口径

本表以 2026-08-30 的三个远程分支快照为准，不把设计、静态配置或 PR 阶段被跳过的 job 记成运行通过：

| 服务 | 分支快照 | 独立服务 Actions | 完整系统 Actions |
|---|---|---|---|
| identity-governance-service | `ea1f1854dbd32cf39291be3204b328f31b96c4e3` | `33297661588` success | `33297661706` success |
| order-service | `3efc7702141596c5e0402e3d4fd4c0889529668b` | `33297930990` success | `33297931045` success |
| secondhand-service | `4f99ea3aa8bbaff01c5e125af66888bf90c9fcb2` | `33300412161` success | `33300412247` success |

上述均为 pull request run，因此 ACR publish、实际 K3s deploy/rollout 和 Helm revision 仍是 `NOT_RUN`，不能由 `success` 总结替代。

## 2. 课程必交项逐项对比

| 检查项 | 身份治理 | 订单 | 二手 | 结论 |
|---|---|---|---|---|
| 独立 Boot/JAR/Flyway/Schema | 已实现并验证 | 已实现；本地报告曾跳过 Docker MySQL 项，远端 Maven job 已通过 | 已实现，MySQL 8.4 权限测试通过 | 三者均具备独立模块；仍应保留远端原始报告 |
| 服务划分图 | MMD + SVG | MMD + SVG | MMD + SVG | 必须项已具备 |
| 服务接口清单/OpenAPI | 34 个 method-path，人工清单与运行时 34/34 对齐 | OpenAPI 与契约测试存在 | OpenAPI 存在 | 二手还缺 OpenAPI/运行时/测试三方的自动数量门禁 |
| 数据表归属表与跨库拒绝 | 归属表 + MySQL 1142 | 归属表与独立 Schema | 归属表 + MySQL 跨库写拒绝 + 架构边界测试 | 必须项；二手的双层边界证据更强，值得保留 |
| 跨服务调用说明 | outbox、内部 API、幂等与失败策略 | catalog/finance/secondhand 契约 | order gateway、business key、outbox、事件幂等、恢复调度 | 必须项；二手因同步建单依赖而需要更细的恢复设计 |
| 改造前后两个代码版本差异 | `monolith-start` 与服务版结构/E2E 同断言 | 有迁移版本说明，微服务后 E2E 报告仍不足 | 有迁移说明，但当前 Domain D E2E 实际仍走根 Compose 单体后端 | 二手不能把现有 5/5 写成“微服务改造后版本 5/5” |
| 所有后端公开接口 API 测试 | 31/31 成功路径，另有鉴权矩阵 | 报告覆盖面广，但未给出 method-path 数量矩阵 | 21 个公开 method-path 中测试源码直接调用 15 个；缺 6 个 | 二手必须补 detail、卖家公开信息/商品、本人列表、删除、上下架六条接口 |
| 用例 E2E | UC01-UC05 单体/微服务两版各 5/5 | 完整系统 Domain C/D 已通过；服务版独立路由证据不足 | Domain D 5/5 是 Nginx + 单体 backend + MySQL，分支未把 secondhand-service 接入根 Compose | 二手必须增加真正路由到 8080 secondhand-service 的改造后 E2E；现有 5/5 只能作为单体回归 |
| 自动构建/测试/制作镜像 | PR 构建 tested JAR 候选镜像；main 从该 JAR 发布 SHA 镜像 | PR Maven/Helm；main Dockerfile 再以 `-DskipTests` 构建 | PR Maven/E2E/Helm；main Dockerfile 再以 `-DskipTests` 构建 | 二手与订单建议补 PR 候选镜像，并避免发布阶段重建未被测试的 JAR |
| 自动 K8s 部署 | main 独立流水线、共享 Helm 串行锁、`--atomic --wait`、失败诊断 | main 原子部署；工作流未实际声明共享 Helm concurrency | main 原子部署；未声明共享 Helm concurrency | 三者 PR 阶段实际部署均未运行；订单/二手合并前应补共享 release 串行锁 |
| 日志、健康、就绪、版本 | JSON 日志，三端点本地实测，部署脚本校验并收集日志 | 端点/手册存在，PR 未产生集群运行证据 | 端点/手册存在；日志只是 pattern，不是 JSON；部署 job 未请求端点、未采集失败日志 | 二手必须补 JSON 日志和 deploy 后 liveness/readiness/info/smoke/失败诊断 artifact |
| 自然语言部署失败说明 | 已有错误 DB 口令本地实跑：1045/SQLState 28000，恢复后验证 | 有排查/回滚手册，实跑证据待补 | 有演练计划，实际 K8s 失败/回滚未运行 | 课程要求最终需要一次真实记录；计划文件不能记为 PASS |

## 3. 二手服务比另外两个服务多出的内容

### 3.1 必须保留或补齐为课程必交

| 二手服务特有内容 | 分类理由 |
|---|---|
| 商品、议价、拍卖 CAS 与并发集成测试 | 直购只能有一个赢家、竞价只能有一个领先者，属于 UC17/UC19 正确性，不是可删的“高级功能” |
| `OrderGateway` HTTP 契约、`tradeType + tradeId` business key | 跨服务不能直写订单库，且重复成交请求必须返回同一订单 |
| `trade_order_request`、`TradeOrderCoordinator`、恢复调度与重试上限 | 同步请求超时结果不确定时必须先查询再重试，否则会重复建单或永久冻结商品 |
| outbox 与事件消费 `eventId` 幂等 | 满足跨服务最终一致性和重复投递安全；但共享 relay/CDC 仍待全系统接入 |
| 自动化订单超时/恢复测试 | 这是服务级失败契约测试，属于跨服务调用质量门，不等于课程后续“停止 K8s 依赖服务”的云原生实验 |
| MySQL 跨库拒绝 | 课程明确要求业务表归属和不得跨服务直接访问数据库 |

### 3.2 建议保留，但不是每个微服务都必须复制

| 内容 | 建议 |
|---|---|
| `ArchitectureBoundaryTest` 与 CI 源码 grep | 作为数据库权限测试的快速前置门禁很好，但它不能替代真实 MySQL 权限测试 |
| `AuctionSettlementScheduler` 与领域分层包结构 | 对拍卖到期结算和复杂交易可维护性有价值；身份服务没有对应业务，不应机械照搬 |
| ConfigMap 中的 order URL、超时、批大小与调度周期 | 适合 K8s 环境调参，Secret 仍只保存敏感值 |
| Actuator `metrics`/`prometheus` | 可为后续性能/HPA 实验准备；当前课程基本运维只强制日志、健康、就绪和版本 |
| 保留首次 UC18 失败原因、修复与最终复测记录 | 课程需要成功和失败记录；保留结构化摘要、关键截图和一份 trace 即可 |

### 3.3 超出本阶段或不建议原样入库

| 内容 | 处理意见 |
|---|---|
| 在真实 K8s 停止/延迟 order-service 的故障实验 | 属于用户明确暂不实施的云原生实验；当前只保留方案和自动化契约测试 |
| HPA 扩缩容运行实验、三轮单体/微服务性能对比 | 等课程实验阶段或全组版本冻结后统一执行，不在当前服务 PR 冒充完成 |
| 暴露 Actuator `flyway` | 对课程验收没有必要，生产环境应删除或仅在受保护的管理网络开放 |
| 将三套 Playwright HTML UI、重复截图、trace/video 全部长期提交 | 当前 evidence 约 101 个文件、28.05 MiB，且 5.39 MiB trace 与多张截图重复；建议 Git 只留摘要/XML/JSON/关键截图，完整 HTML/trace/video 由 Actions artifact 限期保存 |

## 4. 二手服务当前优先修复顺序

1. 补齐 6 条未被 API 测试直接调用的公开接口，并增加 OpenAPI/运行时/测试 method-path 数量门禁。
2. 建立真正的 secondhand-service E2E 路由，复用同一组 UC16-UC20 spec 跑 `monolith-start` 与微服务版，不能继续用单体 Compose 结果代替。
3. PR 阶段构建候选镜像；发布阶段使用已测试 JAR 或可验证的同源制品。
4. 部署 job 增加共享 `segroup8-production-helm` 串行锁、版本/探针/smoke、失败时 Helm/Kubernetes/最近日志采集及 artifact。
5. 将文本 pattern 日志改为 JSON，并收敛 Actuator 暴露面。
6. 将 `.github/workflows/ci-cd-microservices.yml` 改为服务唯一文件名，避免与其他微服务分支合并冲突。
7. 最后才是实际 K3s rollout/回滚证据；HPA、依赖停止实验和性能对比继续保持 `NOT_RUN`。

## 5. HPA 与并行工作边界

当前身份治理分支的 HPA 已完成到“可配置、可静态验证”的程度：已有 `autoscaling/v2` 模板，目标为 `segroup8-identity-governance` Deployment，默认 `min=1`、`max=4`、CPU 70%，Deployment 有 CPU request/limit；CI 会在 HPA 显式启用时渲染并核对关键字段。默认仍关闭，因此没有创建真实 HPA 对象，也没有扩容/缩容运行证据。

课程文字把 HPA 和依赖故障处理分别作为一次系统级云原生实验验收，并没有要求“每个微服务各做一次”。合理做法是选择一个适合水平扩展的无状态服务完成 HPA 实验，再选择一条真实跨服务依赖链完成故障实验；除非教师后续明确补充逐服务要求，否则不应把同一实验机械复制六次。

在其他微服务未完成前，可以独立完成：身份服务镜像和单服务 K3s rollout、Secret/Schema 前置检查、探针/版本/日志验证、静态 HPA 配置、负载脚本与证据模板准备。若以后选身份服务作为课程 HPA 实验对象，只需它自身、数据库、metrics-server 和测试流量，不必等待六个服务全部完成。

必须等待相应协作者而不是等待“所有人”的项目包括：secondhand→order 真实故障实验只需这两个服务；商家审批→店铺创建只需 identity 与 catalog-shop；共享 outbox relay 需要生产者/消费者契约稳定。根网关全量切流、UC01-UC25 微服务版聚合 E2E、全系统性能对比、最终 `microservices-v1` tag 和发布验收则应等待全部目标服务集成完成。
