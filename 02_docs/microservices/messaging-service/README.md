# messaging-service 架构交付

MS-06 负责 UC24–UC25：会话、消息、持久通知、WebSocket、事件 Inbox 去重和投递 Outbox。实现位于 `microservices/messaging-service/`，端口 `8084`，数据库 `messaging_db`。

业务服务在自身事务写 Outbox，再把版本化事件发送到 `/internal/events`。Messaging 先建立唯一 Inbox，再写通知和投递任务；失败使用有上限的退避和 DLQ。离线 WebSocket 投递可等待，REST 历史记录是恢复事实来源。内部接口使用服务令牌，重放接口另用 operations token，不接受浏览器 JWT 代替服务身份。

生产 Ingress 路由 `/api/chat`、`/api/notifications` 和 `/ws/realtime` 到本服务。当前服务具备独立构建、真实 MySQL、公开 API/WebSocket E2E、完整 UC24/25 E2E、不可变镜像、Helm 原子部署、探针、版本和 JSON 关联日志。

材料：

- [服务边界](service-boundary.md)
- [服务图](service-diagram.mmd) / [SVG](service-diagram.svg)
- [OpenAPI](openapi.yaml) / [事件契约](event-contract.md)
- [数据库归属](database-ownership.md)
- [跨服务调用](cross-service-calls.md)
- [改造前后代码差异](before-after-code-diff.md)
- [追溯矩阵](traceability.md)

```bash
mvn -B -f microservices/pom.xml -pl messaging-service -am clean verify
```
