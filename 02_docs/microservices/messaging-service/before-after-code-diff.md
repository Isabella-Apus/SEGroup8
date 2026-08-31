# Messaging 服务改造前后代码差异

## 改造前

- 聊天、通知和实时推送由单体后端直接访问共享业务表。
- 业务事件缺少统一的 Inbox/Outbox、幂等键、重试和 DLQ 边界。
- 前端路由、构建、测试和部署不能独立验证 Messaging 服务。

## 改造后

- `microservices/messaging-service` 独立拥有会话、消息、通知、访问投影、
  Inbox、Outbox 和审计数据，并通过 Flyway 管理自己的 `messaging_db`。
- 公开 REST/WebSocket 接口、内部事件入口和治理回退均使用明确契约；跨服务
  写入改为 EventEnvelope、幂等消费、有限重试和 DLQ。
- Nginx/Vite 将聊天、通知和实时连接定向到 `messaging:8084`，其余 API 保持
  单体回退，便于渐进切流和回滚。
- 独立流水线执行 Maven、真实 MySQL、公开 API、独立服务 E2E、完整系统
  UC24/UC25 E2E、Helm 安全门禁；同一候选镜像经验证后才发布和原子部署。

## 可核验入口

- 实现：`microservices/messaging-service/`
- 接口：`02_docs/microservices/messaging-service/openapi.yaml`
- 数据归属与调用：`02_docs/microservices/messaging-service/`
- 流水线：`.github/workflows/messaging-service-ci-cd.yml`
- 部署：`deploy/helm/segroup8/templates/messaging-*.yaml`
