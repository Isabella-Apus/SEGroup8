# secondhand-service 运维手册

## 本地验收

```bash
mvn -B --no-transfer-progress -f microservices/pom.xml -pl secondhand-service -am clean verify
node scripts/e2e/stubs/secondhand-order-contract-stub.mjs
docker compose -f microservices/secondhand-service/compose.acceptance.yml up -d --build
```

随后在 `frontend/` 执行：

```bash
E2E_BASE_URL=http://127.0.0.1:18080 npx playwright test e2e/microservices/secondhand-service-api.spec.ts --workers=1
```

## 自动交付

生产流程位于 `.github/workflows/secondhand-service-ci-cd.yml`。发布镜像使用 `sha-<完整提交号>`，记录 registry digest；部署使用共享 `segroup8-production-helm` 锁和 `helm upgrade --atomic --cleanup-on-fail --wait`，避免三个微服务同时改写同一个 Helm release。

## 日志、探针与版本

```bash
kubectl -n segroup8 logs deployment/segroup8-secondhand --tail=200
kubectl -n segroup8 logs deployment/segroup8-secondhand --previous --tail=200
kubectl -n segroup8 get pods -l app.kubernetes.io/component=secondhand-service
kubectl -n segroup8 exec deployment/segroup8-secondhand -- curl -fsS http://127.0.0.1:8080/actuator/health/liveness
kubectl -n segroup8 exec deployment/segroup8-secondhand -- curl -fsS http://127.0.0.1:8080/actuator/health/readiness
kubectl -n segroup8 exec deployment/segroup8-secondhand -- curl -fsS http://127.0.0.1:8080/actuator/info
```

生产标准输出为单行 JSON，包含时间、级别、logger、service，并从 MDC 带出 `traceId`、`requestId`、`userId`、`eventId`。禁止记录 JWT、内部令牌、密码。Actuator 仅公开 `health,info,metrics,prometheus`，不公开 Flyway 迁移详情。

## 故障定位顺序

1. 看 Actions 中失败的门禁步骤和 `secondhand-deployment-<sha>` artifact。
2. 用 `helm history/status` 确认失败 revision 与自动回滚状态。
3. 用 `kubectl get/describe` 区分镜像拉取、调度、Secret、探针失败。
4. 同时查看当前与 `--previous` 容器 JSON 日志，使用 `traceId/requestId` 串联请求。
5. 用 `/actuator/info` 对照运行中的 commit 和期望 SHA，避免排查错版本。

订单依赖不可用时，二手服务 readiness 应保持 `UP`；查看 `trade_order_request` 的 `PENDING/RETRY`、尝试次数和 business key。恢复订单服务后，先按 business key 查询再重试，验证没有重复订单。
