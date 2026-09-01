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

## HPA 本地实验

前置条件：Docker Desktop Kubernetes 已启动，`kubectl config current-context` 指向本地测试集群，且 Metrics API 可用。
脚本会创建独立命名空间和临时数据库，不读取生产 Secret，结束后默认自动清理。

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\03_devops\microservices\secondhand-service\run-hpa-preexperiment.ps1 `
  -ExperimentType formal -RunNumber 1 `
  -MinReplicas 1 -MaxReplicas 4 `
  -VUs 15 -Duration 150s `
  -TargetCpuUtilization 70 -ScaleDownTimeoutSeconds 360
```

如果仅在本地测试集群缺少 Metrics Server，可追加 `-InstallMetricsServer`。生产集群必须由平台管理员统一安装，
不得由业务流水线临时修改。脚本默认使用本次运行号构建唯一镜像；在 Docker Desktop 上会把该镜像显式导入
Kubernetes 节点并使用 `imagePullPolicy=Never`，防止同名旧镜像污染实验。`-SkipImageBuild` 只用于调试已知镜像，
不得用于正式结果。

通过条件为初始 1 个 Ready Pod、负载期间出现额外 Ready Pod、Deployment 扩容、停压后回到 1 个，且 k6 的
HTTP 失败率和服务端错误率通过门限。汇总同时记录峰值副本与峰值 Ready 数；两者不同必须在报告中如实说明。
原始证据输出到：

```text
04_tests/microservices/secondhand-service/evidence/hpa/
```

启用 HPA 时 `secondhand-deployment.yaml` 不固定 `replicas`，由 `secondhand-hpa.yaml` 管理。课程配置默认范围为
1–4，CPU 目标为 70%。

## 订单依赖停止与恢复

前置条件：Docker Desktop 正常运行，端口 `18080` 和 `18085` 未被其他程序占用。脚本使用隔离 Compose project、
真实 secondhand-service、MySQL 8.4.6 和独立 HTTP 订单契约进程，不读取生产 Secret。

```powershell
powershell -ExecutionPolicy Bypass -File `
  .\03_devops\microservices\secondhand-service\run-order-dependency-drill.ps1
```

通过条件为：依赖停止时返回 `RETRY` 且 readiness 保持 `UP`；恢复后沿用同一 business key 并只生成一条请求；
达到重试上限时请求为 `FAILED`、商品恢复 `ON_SHELF` 且只有一条失败 outbox。证据输出到
`04_tests/microservices/secondhand-service/evidence/fault-drill/`。

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

本地错误镜像与 Helm 自动回滚已按上述顺序实测，结果和证据索引见
`deployment-failure-drill.md`。生产演练仍使用同一诊断顺序，但不得复制本地测试 Secret 或把本地 revision 写成生产结果。

订单依赖不可用时，二手服务 readiness 应保持 `UP`；查看 `trade_order_request` 的 `PENDING/RETRY`、尝试次数和
business key。恢复订单服务后，先按 business key 查询再重试，验证没有重复订单。

若依赖已恢复但请求长期停在 `RETRY`，同时查询数据库时钟与到期时间：

```sql
SELECT id, request_status, attempts, next_retry_at, CURRENT_TIMESTAMP,
       TIMESTAMPDIFF(SECOND, CURRENT_TIMESTAMP, next_retry_at)
FROM trade_order_request
WHERE request_status = 'RETRY';
```

正式实现使用数据库单一时钟写入和筛选 `next_retry_at`。禁止改回 `LocalDateTime.now()` 后再用
数据库 `CURRENT_TIMESTAMP` 筛选；容器与 MySQL 时区不一致时会形成约 8 小时的隐性恢复延迟。
真实 K3s 修复前后证据见
`04_tests/cloud-native-experiments/20260831-recovery-d6eee99b/`。

暂停恢复任务时应先缩容服务或统一调整调度间隔，处理完成后再恢复副本，并检查 `trade_order_request` 与
`outbox_event` 积压。共享 relay/CDC 接入前，`event_status=NEW` 表示待集成，不得冒充已送达或手工删除。
