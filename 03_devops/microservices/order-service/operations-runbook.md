# 部署、验证与回滚手册

## 部署前

1. `mvn -B -f microservices/pom.xml -pl order-service -am clean verify` 通过。
2. `order-service-jar`、`order-service-candidate-<git-sha>` 和独立服务 E2E 必须来自同一次 workflow run；候选元数据中的 Git SHA、JAR SHA-256、Docker Image ID 必须一致。
3. 镜像必须是 `${ACR_REGISTRY}/${ACR_NAMESPACE}/order:sha-<git-sha>`，记录 registry digest；发布 job 只能加载并推送已通过独立 E2E 的候选镜像，不允许重新执行 Maven 或 Docker build。
4. `segroup8-order-secret` 包含 `DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/INTERNAL_SERVICE_TOKEN`；`order_app` 只拥有 `order_db.*`。
5. Flyway 变更先在同版本 MySQL 预演；当前 V1 只创建本服务表。

## 可追溯制品链

`clean verify → 测试通过的 Boot JAR → 候选镜像归档 → UC11-UC15/UC20 独立服务 E2E → 原镜像推送 ACR → Helm atomic deploy`。

- `order-service-jar`：测试过的 Boot JAR。
- `order-service-candidate-<git-sha>`：候选镜像 tar、JAR checksum 和 `release-metadata.json`。
- `order-service-api-e2e-<git-sha>`：Playwright JSON/JUnit/HTML、运行时 OpenAPI、候选元数据和 Compose 日志。
- `order-image-digest-<git-sha>`：发布目标、registry digest、候选 Image ID/JAR SHA-256。
- `order-deployment-<git-sha>`：部署日志、诊断采集日志和 Kubernetes 诊断压缩包。

## 部署

CI 使用 `helm upgrade --install --atomic --wait --timeout 10m`，设置 `order.enabled=true`、不可变 sha tag、commit/build 元数据。readiness 只检查本地 DB/migration，liveness 不依赖 catalog、finance 或 messaging。

## 验证

```bash
kubectl -n segroup8 rollout status deploy/segroup8-order --timeout=5m
kubectl -n segroup8 get pods -l app.kubernetes.io/component=order-service
kubectl -n segroup8 port-forward svc/segroup8-order 18085:8085
curl -fsS http://127.0.0.1:18085/actuator/health/readiness
curl -fsS http://127.0.0.1:18085/actuator/info
```

订单 smoke 使用专用测试 JWT 调用只读 detail/list；不得将 token 写入日志或报告。

## 回滚

先记录失败 revision、pod events 和日志，再执行 `helm rollback segroup8 <last-good-revision> --wait --timeout 10m`。数据库迁移采用向前修复；不得在生产直接 drop/回滚已被新版本写入的数据列。确认旧版本可读取 V1 schema 后再回滚应用。

## 日志与隐私

控制台每行使用 JSON，HTTP 完成事件含 traceId、requestId、方法、路径、状态和耗时；相关流程追加 orderId、orderNo、sagaId、paymentRequestId、reservationId。服务接受 `X-Request-Id`、`X-Trace-Id` 或 W3C `traceparent`，并回传关联 ID。禁止打印地址全文、JWT、内部 token 和未脱敏金额/资金账号。API 响应只返回手机号掩码和省市摘要。

部署 job 无论成功或失败都会尝试下载 `order-deployment-diagnostics.tar.gz`，其中包含 Helm 状态/历史、workload 列表、Deployment describe、namespace events、当前/上一个容器日志和已部署 manifest；Secret 值不在采集范围内。
