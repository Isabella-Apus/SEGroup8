# 部署、验证与回滚手册

## 部署前

1. `mvn -B -f microservices/pom.xml -pl order-service -am clean verify` 通过。
2. 镜像必须是 `${ACR_REGISTRY}/${ACR_NAMESPACE}/order:sha-<git-sha>`，记录 digest。
3. `segroup8-order-secret` 包含 `DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET/INTERNAL_SERVICE_TOKEN`；`order_app` 只拥有 `order_db.*`。
4. Flyway 变更先在同版本 MySQL 预演；当前 V1 只创建本服务表。

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

日志含 traceId、requestId、orderId、orderNo、sagaId、paymentRequestId、reservationId；禁止打印地址全文、JWT、内部 token 和未脱敏金额/资金账号。API 响应只返回手机号掩码和省市摘要。
