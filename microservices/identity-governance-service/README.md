# identity-governance-service

UC01-UC05 的独立 Spring Boot 服务，默认端口 `8091`，独占 `identity_governance_db`。它复用 `security-contract` 的 JWT claims，不依赖单体 `backend` 源码。

```powershell
mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify
```

本地容器运行、Secret、健康检查和数据库隔离步骤见 `03_devops/microservices/identity-governance-service/README.md`。Kubernetes/Helm/HPA 不在本次范围。
