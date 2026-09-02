# identity-governance-service 架构交付

MS-01 独占 UC01–UC05 的身份与治理事实，公开路径使用 `/api/**`，内部协作使用 `/internal/**`。实现位于 `microservices/identity-governance-service/`，端口 `8091`，数据库 `identity_governance_db`。

当前具备独立 Spring Boot JAR、Flyway、真实 MySQL 测试、Dockerfile、JWT、公开/内部 API、Outbox、JSON 日志、liveness/readiness/info、独立流水线、不可变镜像和 K3s/Helm 原子部署。完整系统 HPA 和 Order 故障是系统级实验，不在本服务重复执行。

材料：

- [服务边界](service-boundary.md)
- [服务图](service-diagram.mmd) / [SVG](service-diagram.svg)
- [OpenAPI](openapi.yaml) / [接口清单](service-api-list.md)
- [数据库归属](database-ownership.md)
- [跨服务调用](cross-service-calls.md)
- [改造前后代码差异](before-after-code-diff.md)
- [追溯矩阵](traceability.md)

```bash
mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify
```
