# order-service 最终测试计划

覆盖 UC11-UC15 与 UC20、33 个公开 OpenAPI 操作、4 个内部操作、状态机、库存/资金契约、幂等、Saga/Outbox、Flyway、真实 MySQL、候选镜像 E2E、完整浏览器回归和 Helm 部署。标准入口：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl order-service -am clean verify
```

课程依赖故障使用 `secondhand-service -> order-service` 典型链路，由隔离 K3s 实验验证，不在每个服务重复注入。
