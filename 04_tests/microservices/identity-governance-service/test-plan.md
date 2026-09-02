# identity-governance-service 最终测试计划

覆盖 UC01-UC05、31 个公开 OpenAPI 操作、6 个内部操作、JWT/权限、Flyway、真实 MySQL、跨库拒绝、独立浏览器 E2E、候选镜像和 Helm 部署。标准入口：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl identity-governance-service -am clean verify
```

流水线必须依次通过 API/MySQL/契约测试、UC01-UC05 独立 E2E、同一候选镜像发布以及原子 K3s 部署；完整系统 UC01-UC25 由共享流水线回归。
