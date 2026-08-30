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

## HPA 本地预实验

前置条件：Docker Desktop Kubernetes 已启动，`kubectl config current-context` 指向本地测试集群，且 Metrics API 可用。
脚本会创建独立命名空间和临时数据库，不读取生产 Secret，结束后默认自动清理。

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\03_devops\microservices\secondhand-service\run-hpa-preexperiment.ps1 `
  -SkipImageBuild -VUs 15 -Duration 150s `
  -TargetCpuUtilization 70 -ScaleDownTimeoutSeconds 360
```

如果仅在本地测试集群缺少 Metrics Server，可追加 `-InstallMetricsServer`。生产集群必须由平台管理员统一安装，
不得由业务流水线临时修改。通过条件为初始 1 个 Ready Pod、负载期间至少 2 个 Ready Pod、停压后回到 1 个，
且 k6 的 HTTP 失败率和服务端错误率都通过门限。原始证据输出到：

```text
04_tests/microservices/secondhand-service/evidence/hpa/
```

启用 HPA 时 `secondhand-deployment.yaml` 不固定 `replicas`，由 `secondhand-hpa.yaml` 管理。课程配置默认范围为
1–4，CPU 目标为 70%。

## Helm 静态检查

```bash
helm lint deploy/helm/segroup8 \
  --set-string backend.image.repository=registry.example/segroup8/backend \
  --set-string backend.image.tag=sha-test \
  --set-string frontend.image.repository=registry.example/segroup8/frontend \
  --set-string frontend.image.tag=sha-test \
  --set secondhand.enabled=true \
  --set secondhand.autoscaling.enabled=true \
  --set-string secondhand.image.repository=registry.example/segroup8/secondhand \
  --set-string secondhand.image.tag=sha-test \
  --set-file mysql.initSchema=backend/src/main/resources/schema.sql
```

## 自动交付

生产流程位于 `.github/workflows/secondhand-service-ci-cd.yml`。发布镜像使用 `sha-<完整提交号>` 并记录
registry digest；部署使用共享 `segroup8-production-helm` 锁和
`helm upgrade --atomic --cleanup-on-fail --wait`，避免多个微服务同时改写同一个 Helm release。生产部署脚本显式启用
`secondhand.autoscaling.enabled=true`。

## 日志、探针与版本

```bash
kubectl -n segroup8 logs deployment/segroup8-secondhand --tail=200
kubectl -n segroup8 logs deployment/segroup8-secondhand --previous --tail=200
kubectl -n segroup8 get pods -l app.kubernetes.io/component=secondhand-service
kubectl -n segroup8 get hpa segroup8-secondhand
kubectl -n segroup8 exec deployment/segroup8-secondhand -- curl -fsS http://127.0.0.1:8080/actuator/health/liveness
kubectl -n segroup8 exec deployment/segroup8-secondhand -- curl -fsS http://127.0.0.1:8080/actuator/health/readiness
kubectl -n segroup8 exec deployment/segroup8-secondhand -- curl -fsS http://127.0.0.1:8080/actuator/info
```

生产标准输出为单行 JSON，包含时间、级别、logger、service，并从 MDC 带出 `traceId`、`requestId`、
`userId`、`eventId`。禁止记录 JWT、内部令牌、密码。Actuator 仅公开 `health,info,metrics,prometheus`，
不公开 Flyway 迁移详情。

## 故障定位顺序

1. 看 Actions 中失败的门禁步骤和 `secondhand-deployment-<sha>` artifact。
2. 用 `helm history/status` 确认失败 revision 与自动回滚状态。
3. 用 `kubectl get/describe` 区分镜像拉取、调度、Secret、HPA 或探针失败。
4. 同时查看当前与 `--previous` 容器 JSON 日志，使用 `traceId/requestId` 串联请求。
5. 用 `/actuator/info` 对照运行中的 commit 和期望 SHA，避免排查错版本。

订单依赖不可用时，二手服务 readiness 应保持 `UP`；查看 `trade_order_request` 的 `PENDING/RETRY`、尝试次数和
business key。恢复订单服务后，先按 business key 查询再重试，验证没有重复订单。

暂停恢复任务时应先缩容服务或统一调整调度间隔，处理完成后再恢复副本，并检查 `trade_order_request` 与
`outbox_event` 积压。共享 relay/CDC 接入前，`event_status=NEW` 表示待集成，不得冒充已送达或手工删除。
