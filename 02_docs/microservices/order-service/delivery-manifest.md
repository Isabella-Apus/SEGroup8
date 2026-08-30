# 交付清单

- 源码：`microservices/order-service/`（唯一 Boot 入口、Flyway、Dockerfile、四类测试目录）。
- 架构：本目录固定十个文件。
- 运维：`03_devops/microservices/order-service/`。
- Helm：`deploy/helm/segroup8/templates/order-*.yaml` 与 `values.yaml`。
- CI/CD：`.github/workflows/order-service-ci-cd.yml`，复用仓库 Variables/Secrets，不保存明文；生产 Helm job 与其他服务共享 `segroup8-production-helm` 串行锁。
- 测试证据：`04_tests/microservices/order-service/`。
- 管理证据：`05_management/microservices/order-service/`。

待外部流程产生后回填：负责人/非作者评审人、Issue/PR URL、merge SHA、镜像 digest、Helm revision、集群截图和三轮 k6 原始结果。
