# catalog-shop-service 运维交付

构建：`mvn -B -f microservices/pom.xml -pl catalog-shop-service -am clean verify`。CI 从已测试 JAR 构建一次候选镜像，经独立 MySQL Compose 与 Helm 验证后保存；发布阶段只加载并推送同一镜像为 `segroup8/catalog-shop:sha-<git-sha>`，不重新构建。部署由 `.github/scripts/deploy-catalog-shop-k3s.sh` 使用 `helm upgrade --install ... --atomic --cleanup-on-fail --wait`，探针为 `/actuator/health/liveness`、`/actuator/health/readiness`，版本为 `/actuator/info`。

详细操作见 `operations-runbook.md`。课程 HPA 统一使用完整系统实验，不在 Catalog 服务重复执行。
