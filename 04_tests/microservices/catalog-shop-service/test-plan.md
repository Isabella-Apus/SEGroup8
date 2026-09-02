# catalog-shop-service 最终测试计划

覆盖 UC06-UC10、30 个公开 OpenAPI 操作、6 个内部操作、库存并发/幂等、Flyway、真实 MySQL、跨库拒绝、独立服务 smoke、完整浏览器 E2E、候选镜像和 Helm 部署。标准入口：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl catalog-shop-service -am clean verify
```

独立流水线验证服务边界和候选镜像；共享流水线执行 UC06-UC10 及完整 UC01-UC25 回归。
