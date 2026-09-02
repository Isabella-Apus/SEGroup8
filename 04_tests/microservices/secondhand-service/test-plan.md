# secondhand-service 最终测试计划

覆盖 UC16-UC20、21 个公开 OpenAPI 操作、2 个内部操作、交易并发、Order 契约与自动恢复、Flyway、真实 MySQL、候选镜像 E2E、完整浏览器回归和 Helm 部署。标准入口：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl secondhand-service -am clean verify
```

二手服务固定使用 `replicaCount`，不再承担课程 HPA；HPA 由完整系统实验验证。Order 停止与恢复由隔离 K3s 实验验证。
