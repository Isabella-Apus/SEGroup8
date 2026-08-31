# catalog-shop-service 运维交付

构建：`mvn -B -f microservices/pom.xml -pl catalog-shop-service -am clean verify`。镜像只在验证成功后构建为 `segroup8/catalog-shop:sha-<git-sha>`。部署使用 `helm upgrade --install ... --atomic --wait`，探针为 `/actuator/health/liveness`、`/actuator/health/readiness`，版本为 `/actuator/info`。

详细操作见 `operations-runbook.md`，失败演练见 `deployment-failure-drill.md`，扩缩容实验见 `hpa-experiment.md`。
