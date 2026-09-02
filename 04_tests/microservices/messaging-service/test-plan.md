# messaging-service 最终测试计划

覆盖 UC24-UC25、8 个公开 OpenAPI 操作、4 个内部操作、WebSocket、JWT/内部鉴权、Inbox/Outbox、重试/DLQ、Flyway、真实 MySQL、候选镜像 E2E、完整系统 E2E 和 Helm 部署。标准入口：

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl messaging-service -am clean verify
```

公开 HTTP 路由、WebSocket 路由和前端代理由 CI 审计；通知投递失败不能回滚已完成的订单或治理事务。
