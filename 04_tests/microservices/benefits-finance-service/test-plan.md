# benefits-finance-service 最终测试计划

覆盖 UC21-UC23 及 UC12/UC14 资金协作、17 个公开 OpenAPI 操作、8 个内部操作、金额/优惠规则、余额与流水原子性、幂等支付/退款/结算、Outbox、Flyway、真实 MySQL、候选镜像 E2E、完整系统回归和 Helm 部署。标准入口：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl benefits-finance-service -am clean verify
```

资金操作必须在 Finance 自有事务中完成，Order 只保存业务结果和外部请求号，不跨库修改余额或优惠券。
