# Kinda Goods 课程最终交付报告

版本日期：2026-09-02
功能与生产实测基线：`main@b622e6bbb0447d6823b50e7789e4777f7131eb9b`

## 1. 交付结论

系统已从兼容单体逐步改造为六个可独立构建、测试、制作镜像和部署的微服务：identity-governance、catalog-shop、order、secondhand、benefits-finance、messaging。生产环境保留 frontend、共享兼容 backend 和 MySQL，用于统一入口、尚未完全切除的兼容路径以及改造前性能基线；六个服务的专属路由已经由 Ingress/Nginx 切向对应微服务。

`main@b622e6bb` 的完整系统和六条服务流水线均成功。每个服务具备独立 Maven 模块、Flyway、Dockerfile、API/集成/E2E、Helm、探针、版本与日志；完整系统具备 UC01-UC25 真实 Compose/Playwright 回归和 K3s 部署。完整 HTML、trace、video 与流水线日志由 Actions artifact 保存，Git 只保留结构化结果、关键截图和最终实验数据。

## 2. 课程要求映射

| 要求 | 交付位置 |
|---|---|
| 需求、概要设计、详细设计 | `02_docs/specifications/` |
| UC01-UC25 模型、测试、追溯 | `02_docs/UC01/` 至 `UC25/` |
| 服务划分图 | `02_docs/architecture/microservice-boundaries.md` |
| 服务接口清单 | `02_docs/architecture/service-api-catalog.md` 与六份 OpenAPI |
| 数据表归属表 | `02_docs/architecture/database-ownership.md` |
| 跨服务调用说明 | `02_docs/architecture/README.md` 与六份 `cross-service-calls.md` |
| 改造前后代码差异 | 六份 `before-after-code-diff.md` |
| Docker、CI/CD、K8s/Helm | `.github/`、`deploy/`、`docker/`、`03_devops/` |
| 测试与运行证据 | 源码测试目录、`frontend/e2e/`、`04_tests/` |
| 性能、HPA、依赖故障实验 | `03_devops/cloud-native-experiments/README.md` 与最终原始数据目录 |

## 3. 微服务划分与数据归属

| 服务 | 核心用例 | 独立数据库 |
|---|---|---|
| identity-governance | UC01-UC05 | `identity_governance_db` |
| catalog-shop | UC06-UC10 | `catalog_shop_db` |
| order | UC11-UC15、UC20 | `order_db` |
| secondhand | UC16-UC19 | `secondhand_db` |
| benefits-finance | UC21-UC23 | `benefits_finance_db` |
| messaging | UC24-UC25 | `messaging_db` |

2026-09-02 重新扫描六套 Flyway：物理表数量依次为 10、11、10、8、8、8，共 55 张。每张业务表只有一个写 owner；稳定 ID、必要快照、内部 API 和版本化事件用于跨服务协作。依赖关系详见跨服务说明。未发现生产代码限定访问其他五个 schema。Finance 中未使用的 Order URL 已移除，避免把 Order 停机错误理解为 Finance 的同步故障。

## 4. 自动化测试与交付

| 流水线 | 实测运行 | 结果 |
|---|---:|---|
| 完整系统 CI/CD | 33526387696 | PASS |
| Identity Governance | 33526387419 | PASS |
| Order | 33526387441 | PASS |
| Secondhand | 33526387403 | PASS |
| Catalog-Shop | 33526387391 | PASS |
| Messaging | 33526387412 | PASS |
| Benefits-Finance | 33526387386 | PASS |

典型服务流水线顺序是：检出代码 → Maven/真实 MySQL/API 契约 → 从唯一 JAR 构建候选镜像 → 独立 API/E2E 和相关完整系统 E2E 复用该候选镜像 → Helm 校验 → `main` 推送不可变镜像 → 在部署开关开启时原子升级 K3s → 检查 rollout、健康、就绪和版本。项目不创建 GitHub Release。

六份 OpenAPI 共登记公开接口 140 个 operation、内部接口 30 个 operation；公开接口由对应服务 API/契约测试覆盖，核心用户链由 UC01-UC25 浏览器 E2E 覆盖。文档存在性与文件 hash 不作为普通代码测试门禁。

## 5. 可观测性和部署失败排查

各服务公开 liveness、readiness、info；Kubernetes 使用 startup/liveness/readiness probe。JSON 日志携带 requestId/traceId，敏感身份只保留掩码或稳定 ID。部署失败时按以下顺序定位：Actions 失败步骤 → Helm history/status → Deployment/Pod events → 当前和前一容器日志 → 探针响应 → Service endpoints → 数据库迁移与权限。

一次真实失败发生在实验环境准备。命名空间尚未创建，说明失败位于写集群之前。脚本启用 `set -euo pipefail`，读取镜像摘要的 `awk` 找到目标后提前退出，使仍输出的 `ctr images list` 收到 SIGPIPE，整条管道被判失败。调整为读完输入后再输出摘要，第二次执行成功，随后 MySQL、单体、Identity、Order、Secondhand 全部通过 rollout/readiness。该记录保留了症状、失败边界、根因、修复和复验。

## 6. 云原生与性能实验

完整系统 HPA 绑定系统共享计算层 `segroup8-backend`，流量从 frontend/Nginx 进入 backend 与 MySQL，并混合访问多个业务入口。正式配置为 CPU 60%、`minReplicas=2`、`maxReplicas=4`，结果 `2 → 4（4 Ready）→ 2`，固定与 HPA 六个计量窗口错误率均为 0。`minReplicas=1` 虽节省一个常驻 Pod，但失去 Pod 级冗余和突发预热空间，最终建议保持 2。

Order 依赖故障在隔离命名空间执行：Order 从 1 缩到 0 时，二手购买返回 HTTP 202/`RETRY`，二手 liveness/readiness 仍 UP；Order 恢复后请求自动变为 `CREATED`，重复请求仍只有一张订单。Identity 不受影响；Catalog 浏览/搜索正常但不能完成新建单；Finance 钱包和券接口正常但收不到新支付/退款；Messaging 聊天和既有通知正常但没有新订单事件；Secondhand 浏览/发布正常而成交请求等待恢复。

性能实验选择普通列表、关键字+价格排序和详情三个接口，在同节点、同 MySQL、相同数据量和资源限制下对单体与微服务各跑三轮。代表性结果：详情吞吐约 7.54 倍、详情平均延迟下降约 86.74%；关键字查询吞吐约 22.74 倍、P95 下降约 93.58%。普通列表的单体前两轮大量超时，因此该项主要比较错误率，不夸大不完整的延迟样本。

## 7. 最终目录与提交方式

仓库已按 `01_source` 至 `06_defense` 建立课程目录。开发期间源码仍在原位置，避免破坏 Maven、Compose、Helm 和 CI。最终压缩包中按 `FINAL_DELIVERY_STRUCTURE.md` 把 backend、frontend、microservices、deploy、docker、sql、scripts、`.github`、Compose 与根配置保持原相对路径复制到 `01_source/SEGroup8/`。`.git`、依赖缓存、构建产物、上传数据、私有配置、Secret 和完整 Playwright 体积型报告不得打包。

Issue/PR 描述、阶段审计、中期稿、迁移过程稿、被替代实验轮次和重复证据均已移至仓库外 `selfwork/SEGroup8-final-prune-20260902/`，可追溯但不参与最终验收。`05_management` 和 `06_defense` 保留入口，等待团队补入真实管理材料与答辩材料。
