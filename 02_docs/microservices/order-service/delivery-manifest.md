# 交付清单

- 源码：`microservices/order-service/`（唯一 Boot 入口、Flyway、Dockerfile、四类测试目录）。
- 架构：本目录固定十个文件。
- 运维：`03_devops/microservices/order-service/`。
- Helm：`deploy/helm/segroup8/templates/order-*.yaml` 与 `values.yaml`。
- CI/CD：`.github/workflows/order-service-ci-cd.yml`，复用仓库 Variables/Secrets，不保存明文；测试 JAR 的 SHA-256 写入候选镜像 label 和 release metadata，同一候选 Image ID 经过独立 UC11-UC15/UC20 E2E 后原样推送；生产 Helm job 与其他服务共享 `segroup8-production-helm` 串行锁。
- 运行时契约：`OpenApiContractTest` 同时比较 Controller 映射、Springdoc `/v3/api-docs` 与本目录 `openapi.yaml`；独立服务 E2E 保存实际 runtime OpenAPI。
- 可观测性与诊断：JSON HTTP 完成日志携带关联/订单/Saga 标识；workflow artifacts 提供服务日志、Playwright 结果、候选元数据、镜像 digest 和 Kubernetes 诊断包。
- 测试证据：`04_tests/microservices/order-service/`。
- 管理证据：`05_management/microservices/order-service/`。

待外部流程产生后回填：负责人/非作者评审人、Issue/PR URL、merge SHA、镜像 digest、Helm revision、集群截图和三轮 k6 原始结果。
