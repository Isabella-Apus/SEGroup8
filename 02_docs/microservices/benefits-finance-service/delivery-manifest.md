# 交付清单

| 类别 | 路径 | 状态 |
|---|---|---|
| 源码/迁移/测试/Docker | `microservices/benefits-finance-service/` | 已交付 |
| 架构与 OpenAPI | `02_docs/microservices/benefits-finance-service/` | 已交付 |
| 运维 Runbook | `03_devops/microservices/benefits-finance-service/` | 已交付 |
| Helm | `deploy/helm/segroup8/` | 已交付 |
| CI/CD | `.github/workflows/benefits-finance-service-ci-cd.yml` | 独立命名工作流已交付，远端运行待 PR |
| 独立服务 E2E | `microservices/benefits-finance-service/compose.acceptance.yml`、`frontend/e2e/microservices/benefits-finance-service-api.spec.ts` | 候选镜像 + 独立 MySQL 8.4.6 + 严格 messaging-compatible event stub，3/3 PASS |
| Domain E E2E | `frontend/e2e/domain-e/` | 旧平台 frontend/backend/database 兼容回归 3/3 PASS；当前根 Compose 未含 order/messaging/finance 切流，归属 UC 的真实 finance 路由 E2E 为 `NOT_RUN` |
| 测试证据 | `04_tests/microservices/benefits-finance-service/` | 服务 26/26、安全契约 5/5、MySQL Testcontainers 4/4、独立候选 Playwright 3/3 PASS；完整微服务路由 E2E `NOT_RUN` |
| 管理证据 | `05_management/microservices/benefits-finance-service/` | 模板已交付，负责人/PR/Review 待团队填写 |

## 与三个已交付微服务分支对齐

| 基线分支 | 已核对的共同硬门禁 | 本服务对应实现 |
|---|---|---|
| `origin/feature/ms-order` | 独立命名 CI、真实 MySQL、唯一候选镜像、独立/完整 E2E、精确镜像发布、共享 Helm 串行锁 | 本地门禁已对齐；真实 order→finance 路由仍 `NOT_RUN` |
| `origin/feature/ms-identity-governance` | `clean verify`、UID/GID `10001:10001`、JAR SHA/镜像 ID 校验、原子部署 | 本地门禁已对齐 |
| `origin/feature/ms-secondhand` | 独立 Compose E2E、Domain E E2E、Helm lint/template、失败证据 artifact | 独立 E2E/Helm 已对齐；完整切流仍 `NOT_RUN` |

对齐的是三条分支共同采用的本地交付硬门禁，而不是宣称已完成全部生产切流。服务专属的 UC、数据库、stub 和探针仍按 MS-05 实现。外部状态（Issue、非作者 Review、registry digest、Helm revision）无法由本地代码生成，在取得真实值前分别保持 `PENDING` 或 `NOT_RUN`。
