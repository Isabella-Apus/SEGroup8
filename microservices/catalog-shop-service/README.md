# catalog-shop-service

MS-02 的唯一可部署 Spring Boot 单元，整合 catalog、shop、risk、behavior 与 inventory。运行时只构建本目录的一个可执行 JAR 和一个镜像。

同级的 `catalog-service`、`shop-service`、`risk-service`、`behavior-service` 是远程仓库既有的 Domain B 迁移原型，仍由旧 Domain B 回归测试引用，但不在 Compose 或 Helm 中部署。本服务不调用这些旧进程，也不保留旧公开路径。

```bash
mvn -B -f microservices/pom.xml -pl catalog-shop-service -am clean verify
docker build -t catalog-shop:acceptance microservices/catalog-shop-service
docker compose -f microservices/catalog-shop-service/compose.acceptance.yml up -d --wait
```

自动化测试按 `src/test/{unit,api,integration,contract}` 分层；MySQL 集成测试在 Docker 可用时验证空库 migration 和应用账号跨库拒绝。CI 只发布上述验证阶段保存的候选镜像，不在发布阶段重新构建。

架构、运维和证据分别位于 `02_docs/microservices/catalog-shop-service/`、`03_devops/microservices/catalog-shop-service/` 和 `04_tests/microservices/catalog-shop-service/`。
