# identity-governance-service 交付索引

本目录是 MS-01 的唯一架构交付入口。服务独占 UC01-UC05 的身份与治理事实，公开路径继续使用 `/api/**`，内部协作使用 `/internal/**`。实现位于 `microservices/identity-governance-service/`，默认端口 `8091`，数据库为 `identity_governance_db`。

## 本次范围

- 已实现：独立 Spring Boot JAR、Flyway、MySQL/H2 测试、Dockerfile、本地 Compose、JWT、UC01-UC05 公开 API、内部 API、outbox、健康/就绪/版本信息和 JSON 日志。
- 已验证：Maven 独立测试、真实 MySQL 迁移、核心业务 API/鉴权/非法状态测试。
- 本次不做：Kubernetes/Helm、HPA 自动扩缩容、云上发布、依赖故障注入和性能对比；这些项目在报告中标为 `OUT_OF_SCOPE/NOT_RUN`。

## 文件导航

- `service-boundary.md`：职责与迁移边界
- `service-diagram.mmd` / `service-diagram.svg`：可编辑源图与可查看图
- `openapi.yaml`：公开和内部 API 契约
- `database-ownership.md`：表归属与权限验证
- `cross-service-calls.md`：事件、幂等和失败处理
- `migration-version-report.md`：单体基线与迁移状态
- `traceability.md`：UC 到代码和测试的追溯
- `delivery-manifest.md`：交付状态与复现命令
