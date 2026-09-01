# 部署测试报告

本地完成：Dockerfile、Helm 模板、liveness/readiness/info 配置和原子发布工作流静态交付；`DeliveryArtifactsTest` 已解析 OpenAPI、Chart、values、CI YAML 与 SVG。CI 还会执行 `helm lint`、`helm template`，并把渲染结果作为镜像构建前门禁和原始 artifact。

本地候选镜像验证状态：`PASS`，最终微服务验收状态：`BLOCKED_NOT_READY_FOR_FINAL_ACCEPTANCE`。Docker Desktop 4.67.0 / Engine 29.3.1 可用；镜像用户 `10001:10001`，healthcheck、revision/source/JAR hash 标签和 `/actuator/info` 均已检查。候选镜像独立 Compose（服务 + MySQL 8.4.6 + 严格 messaging-compatible event stub）API E2E 为 3/3 PASS；平台 Compose（frontend/backend/database 均 healthy）旧 Domain E UI 回归也为 3/3 PASS。当前根 Compose 未包含 order-service、messaging-service 或 finance 流量切换，因此真实 finance 路由浏览器 E2E 为 `NOT_RUN`。`helm lint` 和带共享 values 的 `helm template` 均已本地实际通过，并渲染 `ORDER_SERVICE_URL=http://segroup8-order:8085`、`OUTBOX_EVENT_SINK_URL=http://messaging:8084/internal/events`。原始证据位于 `evidence/{independent-e2e,compose-e2e}/`。

Helm 静态验证由本地自动化测试解析 Chart/values/template；迁移到最新 main 后将再次执行 `helm lint`/`helm template`。真实 Kubernetes 集群状态：`NOT_RUN`。待 CI/测试集群记录：

- image `sha-<git-sha>` 与 digest；
- Helm revision、`helm --atomic` 结果；
- Pod probes、events、rollout；
- 只读余额 smoke；
- 回滚 revision 与恢复时间。

工作流已经将 readiness、`info.version`、`balance` 表只读查询和 Helm revision 记录设为部署硬门禁，并上传包含 image、digest、Git SHA、Helm revision 和部署时间的 metadata artifact。registry digest 和 Helm revision 在发布前保持 `PENDING`；真实集群未执行前 rollout 保持 `NOT_RUN`，不得用工作流定义或示例输出冒充运行证据。
