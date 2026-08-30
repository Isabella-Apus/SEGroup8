# feat(ms-01): 交付身份治理微服务并接入完整系统 K3s CI/CD

## 关联任务

Refs: MS-01 `identity-governance-service`，覆盖 UC01-UC05。本 PR 不使用 `Closes`，Issue 编号和非作者评审人由团队补充。

## 交付结果

本 PR 从单体身份与平台治理模块中拆出可独立构建、测试、制作镜像和部署的 `identity-governance-service`。服务独占 `identity_governance_db`，保留原 `/api/**` 契约，通过 JWT、本地验签契约、内部最小 API 和事务 outbox 与其他服务协作。

Kubernetes 部署已经纳入现有完整系统 `Kinda Goods CI/CD`：PR 阶段执行 Maven、真实 MySQL、API 契约和候选镜像构建；合并到 `main` 后，在全系统测试通过的前提下推送 ACR 不可变镜像，并随 `segroup8` Helm release 执行 `--atomic --wait` 部署、探针/版本/smoke 验证和失败诊断上传。

暂不执行的只有课程中的两个云原生实验：HPA 自动扩缩容实验、停止或延迟依赖服务的故障处理实验。HPA 模板已提供但默认关闭。这不代表 Kubernetes 部署未完成。

## 服务边界与数据

- UC01：注册、登录、JWT 签发和角色边界。
- UC02：用户资料、搜索和地址所有权。
- UC03：商家申请、审批/驳回和 `MerchantApproved.v1` outbox。
- UC04：用户查询、封禁/解禁、`UserAccessChanged.v1` 和管理审计。
- UC05：举报、拉黑、信用调整和治理审计。
- 独占表：`user`、`address`、`merchant_application`、`user_report`、`user_block`、`credit_score_log`、`admin_audit_log`、`idempotency_record`、`outbox_event`；`report` 仅作旧数据归档。
- 服务账号只允许访问 `identity_governance_db.*`；跨查 `order_db.order_info` 的本地验证返回 MySQL 1142。

## 接口与测试

- OpenAPI/运行时：34/34 method-path 一致，其中公开 31、内部 3。
- 公开接口：31/31 有成功路径 API 断言。
- 受保护接口：29/29 有匿名拒绝和已删除账户旧 JWT 拒绝断言。
- 管理接口：10/10 有普通用户越权拒绝断言。
- 真实 MySQL 测试经过 Controller → JWT → Service → JDBC → Flyway Schema。
- UC01-UC05 Playwright：改造前 `monolith-start` 5/5 PASS；改造后微服务栈 5/5 PASS，复用完全相同的 spec。

## CI/CD 流程

1. 完整系统主工作流调用 `.github/workflows/ci-cd-microservices.yml`。
2. Java 17 + Maven 执行单元、API、契约、H2 和 Testcontainers MySQL 测试，失败立即停止。
3. 构建候选镜像并上传 Boot JAR/Surefire 原始报告。
4. 仅 `main` 且前置全系统门禁通过后，登录现有 ACR 并推送 `identity-governance:sha-<full-sha>`，保存镜像 digest。
5. 复用现有 SSH/K3s 发布路径，检查 `identity-governance-secret`，执行 Helm 原子升级。
6. 等待 `segroup8-identity-governance` rollout，验证 liveness、readiness、`/actuator/info` 版本和公开登录失败 smoke。
7. 失败时记录 Helm status/history、Pod/Service/Ingress、Deployment describe 和最近 200 行服务日志并上传 artifact。

集群一次性前置条件：创建 `identity_governance_db`、仅访问该 Schema 的数据库账号，以及包含 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`INTERNAL_SERVICE_TOKEN`、`BOOTSTRAP_ADMIN_PASSWORD` 的 `identity-governance-secret`。仓库中没有 Secret 明文。

## 可观测性与失败排查

- `/actuator/health/liveness`
- `/actuator/health/readiness`（包含数据库）
- `/actuator/info`（`app.version`、`git.commit`、`build.time`）
- JSON 日志包含 timestamp、level、service、traceId、requestId、eventId，不记录密码/JWT/服务 Token。

本地实际执行了错误数据库口令演练：MySQL 仍为 healthy，服务 Exited(1)，readiness 不可达，日志定位到 SQLState 28000 / MySQL 1045。恢复正确口令并重建服务后，容器 healthy、liveness/readiness 均 UP、版本可读、注册登录 smoke 均成功。K3s 的错误 Secret + Helm 自动回滚现场结果需合并后的远端 run 证明。

## 主要文件变更

### 服务实现

- `microservices/identity-governance-service/pom.xml`
- `microservices/identity-governance-service/Dockerfile`
- `microservices/identity-governance-service/compose.local.yml`
- `microservices/identity-governance-service/compose.failure-drill.yml`
- `microservices/identity-governance-service/src/main/java/com/segroup8/identity/**`
- `microservices/identity-governance-service/src/main/resources/application.yml`
- `microservices/identity-governance-service/src/main/resources/logback-spring.xml`
- `microservices/identity-governance-service/src/main/resources/db/migration/V1__identity_governance_schema.sql`
- `microservices/pom.xml`

### 自动测试

- `src/test/.../api/AuthenticationApiTest.java`
- `src/test/.../api/PublicApiSuccessCoverageTest.java`
- `src/test/.../contract/PublicApiSecurityContractTest.java`
- `src/test/.../contract/InternalApiContractTest.java`
- `src/test/.../integration/IdentityGovernanceFlowIntegrationTest.java`
- `src/test/.../integration/MySqlMigrationIntegrationTest.java`
- `src/test/.../unit/TokenContractUnitTest.java`
- `04_tests/microservices/identity-governance-service/**`

### CI/CD 与 Kubernetes

- `.github/workflows/ci-cd-microservices.yml`
- `.github/workflows/ci-cd.yml`
- `.github/scripts/deploy-k3s.sh`
- `deploy/helm/segroup8/values.yaml`
- `deploy/helm/segroup8/templates/identity-governance-deployment.yaml`
- `deploy/helm/segroup8/templates/identity-governance-service.yaml`
- `deploy/helm/segroup8/templates/identity-governance-hpa.yaml`
- `deploy/helm/segroup8/README.md`

### 架构、运维与管理资料

- `02_docs/microservices/identity-governance-service/**`：服务图及 SVG、边界、OpenAPI、接口清单、表归属、跨服务调用、前后版本差异、追溯和交付清单。
- `03_devops/microservices/identity-governance-service/**`：运行手册、数据库隔离和部署失败排查。
- `05_management/microservices/identity-governance-service/**`：分支、PR、评审和未完成项记录。

## 本地验证结果

| 门禁 | 结果 |
|---|---|
| Maven reactor | PASS |
| `security-contract` | 5/5 PASS |
| 身份服务测试 | 15/15 PASS |
| 真实 MySQL 测试 | 2/2 PASS，未跳过 |
| OpenAPI/运行时 | 34/34 PASS |
| 全公开接口成功/安全矩阵 | PASS |
| 数据库跨 Schema 拒绝 | PASS，MySQL 1142 |
| 微服务 Domain A E2E | 5/5 PASS |
| `monolith-start` 同断言 E2E | 5/5 PASS |
| 本地镜像 | PASS，`USER=10001:10001`，readiness healthcheck |
| 本地错误口令失败/恢复 | PASS |
| 文档门禁 | PASS，25/25 UC |
| UC E2E 静态覆盖门禁 | PASS，25/25 |
| Helm lint/template | 等待 PR Actions（本机未安装 Helm） |
| ACR 推送与 K3s 部署 | 合并到 `main` 后运行；PR 阶段 NOT_RUN |

## 尚未完成/不在本 PR 中冒充完成

- 非作者 Review、merge commit、`microservices-v1` tag。
- ACR/K3s 远端成功证据（只能由合并后的 Actions run 产生）。
- HPA 自动扩缩容实验与指标记录。
- 停止/延迟依赖服务的故障处理实验。
- 全组统一的单体/微服务性能对比。
- 身份数据正式导入与根网关流量切换；在对账前不长期双写，也不直接跨库。
