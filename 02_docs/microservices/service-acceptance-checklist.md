# 微服务最终验收固定检查清单

适用范围：后续所有业务微服务。以身份治理、订单、二手服务已交付流程为基线。检查时必须填写实际文件、命令、结果与证据路径；配置存在不等于运行通过，未执行统一写 `NOT_RUN`。

## 0. 验收元数据

- [ ] 服务名、负责人、分支、PR、基线 commit、当前 commit、验收日期已记录。
- [ ] 对应 UC 编号和验收边界已记录；无归属 UC 的公开接口也纳入 API 契约测试。
- [ ] 标明本次结论：`PASS`、`FAIL`、`BLOCKED` 或 `NOT_RUN`，禁止用“预计可用”替代结果。
- [ ] 保存可复现命令、原始 JSON/XML/日志和关键失败证据；不得只提交截图或口头结论。

## 1. 服务划分与课程文档（必查）

- [ ] 有服务划分图，标出调用方、服务、数据库、外部依赖及调用方向；源文件优先 Mermaid，并提供可查看版本。
- [ ] 有服务边界说明：本服务负责什么、不负责什么、业务一致性边界是什么。
- [ ] 有服务接口清单/OpenAPI：方法、路径、鉴权角色、幂等键、请求/响应、错误码、对应 UC、调用方。
- [ ] 有数据表归属表：每张业务表唯一归属一个服务；说明主键、敏感字段、迁移版本和其他服务的读取方式。
- [ ] 有跨服务调用说明：调用方向、同步/异步、超时、重试、幂等、失败语义、恢复方式和关联 ID。
- [ ] 有改造前后两个代码版本差异：基线 tag/commit、当前 commit、目录/模块/路由/表归属/部署方式差异及可复现 `git diff` 命令。
- [ ] 有 UC→接口→代码→自动化测试→证据的追踪表。

建议目录：

```text
02_docs/microservices/<service>/
  README.md
  service-boundary.md
  service-diagram.mmd
  openapi.yaml
  database-ownership.md
  cross-service-calls.md
  traceability.md
  migration-version-report.md
  delivery-manifest.md
```

## 2. 独立性与源码边界（必查）

- [ ] 服务有独立构建模块、唯一应用入口、独立配置、独立迁移脚本、Dockerfile 和测试目录。
- [ ] 能从仓库根目录使用单条命令独立构建测试，例如：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl <service> -am clean verify
```

- [ ] CI 有源码边界门禁，禁止直接引用其他领域 Mapper/Repository/Entity 或写入其他服务的表。
- [ ] 跨服务数据只能通过已登记接口/消息契约获取；没有跨库 join、共享业务表或绕过接口的 SQL。
- [ ] 服务可单独启动；验收环境使用真实服务镜像、独立数据库和最小下游契约 stub，不依赖单体后端伪装成该服务。

## 3. 数据库与迁移（必查）

- [ ] 使用独立 schema/database 和最小权限账号。
- [ ] Flyway/Liquibase 迁移从空库可重复执行，版本单调、不修改已发布迁移。
- [ ] 使用与生产同主版本的真实 MySQL 集成测试，不只使用 H2。
- [ ] 有跨 schema 权限负测试，证明服务账号不能读取/写入其他领域表。
- [ ] CI 拒绝跳过必需集成测试；Docker/Testcontainers 不可用时应失败，不得静默变成绿色。
- [ ] 数据迁移失败不会让 readiness 误报 `UP`。

## 4. 接口、鉴权与契约（必查）

- [ ] Controller 运行时路由集合与审核清单精确相等；新增/删除公开路由会使契约测试失败。
- [ ] 运行时 `/v3/api-docs` 与仓库 OpenAPI 的方法+规范化路径集合精确相等。
- [ ] 所有后端公开接口至少有一个 API 自动化成功路径；同时覆盖未认证、越权、参数错误和资源不存在。
- [ ] 内部接口使用独立服务令牌/服务身份，不把浏览器用户 JWT 当服务间凭证。
- [ ] 写接口定义并测试 `Idempotency-Key`；相同键+相同请求返回同一结果，相同键+不同请求明确拒绝。
- [ ] 错误响应格式稳定，包含机器可读 code；未捕获异常不泄露堆栈、SQL、Secret 或内部拓扑。
- [ ] Actuator 和 OpenAPI 不被业务异常处理器错误转换成 500。

建议硬门禁：

```text
运行时公开操作数 = 审核 OpenAPI 公开操作数 = API 测试覆盖操作数
```

别名路由也算公开接口，必须登记并测试；不能只统计 Controller 方法数。

## 5. 下游调用与一致性（必查）

- [ ] 每个同步下游设置连接超时和读取超时，不使用无限等待。
- [ ] 明确哪些错误可重试；重试有上限、退避和幂等键，禁止无界重试。
- [ ] 对远程结果未知（请求超时但可能已成功）先按业务键查询，再决定重试，避免重复扣款/下单/发放。
- [ ] 本地事务与远程调用的边界已说明；需要最终一致性时使用 outbox、状态机、补偿或恢复任务。
- [ ] 服务启动和 readiness 不把非关键下游短暂不可用当成本服务死亡；数据库等关键依赖应影响 readiness。
- [ ] 契约 stub 校验路径、方法、内部令牌、幂等键和关键请求体，而不是对任意请求统一返回 200。

## 6. 日志、健康、就绪和版本（必查）

- [ ] 生产控制台输出单行 JSON；固定字段至少有 timestamp、level、service、logger、message。
- [ ] HTTP 完成日志含 requestId/traceId、method、path、status、durationMs；领域日志含必要的业务关联 ID。
- [ ] 日志不输出密码、令牌、完整手机号/地址或支付敏感信息。
- [ ] `/actuator/health/liveness` 可访问，仅判断进程是否存活。
- [ ] `/actuator/health/readiness` 可访问，包含关键数据库检查。
- [ ] `/actuator/info` 返回部署的 version、commit、buildTime；流水线冒烟校验 commit 与预期 SHA 一致。
- [ ] 只暴露批准端点（通常 `health,info,metrics,prometheus`）；生产不暴露 `flyway`、`env`、`configprops`、`beans`、`heapdump`。
- [ ] 自动化测试验证 Actuator 根链接不含 flyway，且直接访问 `/actuator/flyway` 返回 404。
- [ ] 能通过 `kubectl logs` 查看当前日志和 `--previous` 上一容器日志。

## 7. 测试分层（必查）

- [ ] 单元测试：状态机、校验、计算和纯领域规则。
- [ ] API 测试：全部公开操作及鉴权/错误/幂等边界。
- [ ] 契约测试：运行时 OpenAPI、下游 HTTP、错误语义及安全契约。
- [ ] 集成测试：真实 MySQL、迁移、唯一键/事务、数据库权限。
- [ ] 独立服务 API E2E：测试候选镜像 + 独立 MySQL + 严格契约 stub，覆盖本服务全部 UC。
- [ ] 完整系统 Playwright E2E：浏览器经真实前端/代理/后端/数据库完成归属 UC；先跑平台 smoke。
- [ ] 所有测试进程退出码被保留；不得用 `|| true` 掩盖测试失败。`|| true` 只允许在 `if: always()` 的诊断收集中使用。
- [ ] 测试报告记录测试数、失败数、跳过数、环境、commit 和耗时；未运行项明确 `NOT_RUN`。

## 8. 构建与镜像供应链（必查）

- [ ] Maven `clean verify` 先产生唯一 Boot JAR；Dockerfile 只复制该已测试 JAR，不在镜像阶段再次 `-DskipTests` 重编译。
- [ ] 保存 JAR SHA-256、Git SHA、候选镜像 ID/label；独立 E2E 验证的是这一个候选镜像。
- [ ] 发布阶段优先原样加载、打 tag、推送 E2E 已验证的候选镜像，不重新构建；至少必须证明发布镜像包含同一 JAR 哈希。
- [ ] 镜像 tag 不可变，例如 `sha-<full commit>`；发布后保存 registry digest。
- [ ] 容器使用固定数字 UID/GID（基线 `10001:10001`），不是 root，也不依赖用户名映射。
- [ ] 镜像带 revision/source/JAR hash 标签，包含容器 healthcheck，基础镜像与 JRE 版本固定。
- [ ] `.dockerignore` 排除源码外无关文件、测试报告、凭证、Git 元数据和本地缓存。

## 9. 每服务独立 CI/CD（必查）

- [ ] 每个微服务有独立命名工作流：`<Service> Service CI/CD`，job 和 artifact 也带服务名，避免页面混淆。
- [ ] path filter 覆盖服务源码、共享安全契约、对应 E2E、stub、文档、Helm 模板、部署脚本和工作流自身。
- [ ] PR/功能分支自动执行：边界门禁 → Maven/真实 MySQL → 候选镜像 → 独立服务 E2E → 完整系统 UC E2E → Helm lint/template。
- [ ] 仅 main 且前置门禁全部通过后发布 SHA 镜像；功能分支不发布、不部署。
- [ ] 仅 main、生产 Environment 审批及显式开关开启后部署。
- [ ] 三个及更多服务共用一个 Helm release 时，生产部署必须使用统一串行锁（如 `segroup8-production-helm`），防止并发 `helm upgrade` 覆盖 values。
- [ ] 流水线权限最小化，Secret 只来自 GitHub Secrets/Environment，不写入仓库和 artifact。

标准流水线：

```text
verify
  ├─ independent-service-e2e
  ├─ full-system-use-case-e2e
  └─ helm-lint-template
          ↓ 全部通过且 main
publish exact candidate image + digest
          ↓ 生产审批/开关
atomic Helm deploy + smoke + diagnostics artifact
```

## 10. Kubernetes 交付（必查）

- [ ] Helm/清单包含 Deployment、Service、ConfigMap/Secret 引用；需要入口时包含 Ingress 路由。
- [ ] 有 startup、liveness、readiness 三类探针，端口和路径与实际服务一致。
- [ ] 设置 requests/limits；容器 `runAsNonRoot`、禁止提权、drop capabilities，并为只读根文件系统提供必要临时卷。
- [ ] 版本、commit、buildTime 使用服务自己的 values 节点，防止一个服务部署覆盖另一服务版本信息。
- [ ] 下游 Kubernetes DNS 与真实 Service 名称一致；部署前检查关键 Secret/依赖存在。
- [ ] Helm 使用 `--atomic --cleanup-on-fail --wait --timeout ... --history-max ...`，并验证 rollout。
- [ ] 部署后冒烟：liveness、readiness、info SHA、一个公开接口（含正确的 200/401 预期）。
- [ ] 配置静态通过只写 `HELM_STATIC_PASS`；没有目标集群 rollout 证据时仍写 `K8S_DEPLOY_NOT_RUN`。

## 11. 部署失败定位与回滚（必查）

- [ ] 失败时自动上传诊断 artifact：workflow 步骤、Helm status/history、Pod/Service/Ingress、events、describe、当前日志、previous 日志、镜像和探针状态。
- [ ] 能从 artifact 用自然语言复盘一次失败：哪个门禁失败 → 哪个 Pod/事件异常 → 哪段日志/探针说明根因 → 是否自动回滚 → 修复后如何验证。
- [ ] 不把 SSH 断开直接归因于应用；先区分网络/权限、镜像拉取、调度、Secret、迁移、启动、readiness 和业务 smoke。
- [ ] 回滚后检查运行镜像、`/actuator/info` commit、readiness 和关键接口，确认不是只回滚了 Helm 状态。

固定叙述模板：

```text
Actions 在 <步骤> 失败；Helm history 显示 revision <n> 状态为 <状态>。
kubectl describe 的 <事件> 指向 <候选原因>，容器当前/previous JSON 日志中的
requestId=<id> 或启动异常进一步确认根因是 <根因>。由于使用 --atomic，release
已/未回滚到 revision <n-1>。修复 <改动> 后，rollout、readiness、info 中的 commit
和 <公开接口> 冒烟均通过，因此确认恢复。
```

## 12. 证据与仓库体积（必查）

- [ ] Git 只长期保留：摘要、JSON/XML、关键日志、必要的一张关键失败截图和人工结论。
- [ ] Playwright HTML、完整 `test-results`、重复截图、trace.zip、video.webm、Compose 全量日志放 Actions artifact，设置 7–30 天保留期。
- [ ] `.gitignore` 阻止 HTML 报告、trace、video 和临时测试目录再次提交。
- [ ] artifact 名含服务名和 SHA；失败时 `if: always()` 上传，成功/失败均可追溯。
- [ ] 不把旧分支静态报告当本次运行结果；报告的 commit/时间/环境必须与证据对应。

## 13. 验收结论门槛

仅当以下各项都有当前 commit 的证据时，才能写“该微服务达到最终验收状态”：

- [ ] 文档六件套完整：边界/图、接口、表归属、跨服务、前后差异、追踪表。
- [ ] Maven、全部公开 API、真实 MySQL、独立服务 E2E、归属 UC 浏览器 E2E 全部通过且零跳过。
- [ ] 已测试候选镜像、非 root、Helm 静态验证通过。
- [ ] main 发布得到 registry digest。
- [ ] 目标 K8s 完成原子部署，日志、三类探针、版本和公开接口冒烟通过。
- [ ] 有一次可解释的失败/回滚证据，或明确按课程阶段标为待做；不得伪造演练结果。

推荐机器可读状态：

```json
{
  "revision": "<full-sha>",
  "maven": "PASS|FAIL|NOT_RUN",
  "mysqlIntegration": "PASS|FAIL|NOT_RUN",
  "publicApiCoverage": {"covered": 0, "total": 0},
  "independentServiceE2E": "PASS|FAIL|NOT_RUN",
  "fullSystemUseCaseE2E": "PASS|FAIL|NOT_RUN",
  "candidateImage": "PASS|FAIL|NOT_RUN",
  "helmStatic": "PASS|FAIL|NOT_RUN",
  "registryDigest": "<digest>|PENDING",
  "kubernetesRollout": "PASS|FAIL|NOT_RUN"
}
```

## 14. 课程云原生实验（与普通微服务验收分开）

- [ ] HPA 和依赖故障注入使用单独报告、脚本、原始指标与结论，不复制到每个微服务重复做。
- [ ] HPA 正式实验至少记录固定/自动扩缩两组、同机同数据同脚本、各三轮，以及吞吐、平均、P95、错误率和 1→N→1 副本时间线。
- [ ] 依赖故障实验选择一条有代表性的调用链，执行停止或延迟依赖，验证调用方可控降级、非依赖接口可用、无级联失败、恢复后无重复业务写入。
- [ ] 预实验只证明脚本/环境可用，不能替代最终同条件多轮课程实验。
