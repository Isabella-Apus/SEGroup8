# 操作与回滚手册

## 发布前

1. CI 必须通过 `mvn ... clean verify`，且 MySQL Testcontainers 报告必须至少执行一项、0 失败、0 错误、0 跳过；否则禁止构建/发布镜像。
2. 镜像使用不可变标签 `sha-<git-sha>`，记录 registry digest。
3. 确认 `benefits-finance-secrets` 已存在且密钥格式正确；不得导出明文。
4. `helm lint` 和 `helm template` 必须通过并保存渲染 artifact。
5. 使用迁移账号预演 Flyway，运行账号只保留业务 DML 权限。

## 部署

```bash
helm upgrade --install segroup8 deploy/helm/segroup8 \
  --namespace segroup8 --create-namespace --atomic --cleanup-on-fail --wait \
  --timeout 5m --history-max 10 \
  --set benefitsFinance.image.repository="$IMAGE_REPOSITORY" \
  --set benefitsFinance.image.tag="sha-$GIT_SHA"
```

检查：

```bash
kubectl -n segroup8 rollout status deployment/benefits-finance --timeout=180s
kubectl -n segroup8 get pods,events --sort-by=.metadata.creationTimestamp
kubectl -n segroup8 port-forward service/benefits-finance 18085:8085
curl -fsS http://127.0.0.1:18085/actuator/health/liveness
curl -fsS http://127.0.0.1:18085/actuator/health/readiness
curl -fsS http://127.0.0.1:18085/actuator/info
```

readiness 必须同时覆盖数据库、Flyway schema 和必要密钥；`info.app.version` 必须等于部署镜像的 `sha-<git-sha>` 标签。自动部署还会在命名空间内启动一次性 MySQL 客户端 Pod，使用既有 Secret 执行只读 `SELECT COUNT(*) FROM balance`；不打印或导出数据库口令。任一 smoke 失败会使发布 Job 失败，`--atomic` 负责回滚失败 revision。

成功后下载 `benefits-finance-delivery-<git-sha>` artifact，核对并归档 image、registry digest、Git SHA、Helm revision 和 UTC 部署时间。

## 回滚

```bash
helm -n segroup8 history segroup8
helm -n segroup8 rollback segroup8 <REVISION> --wait --timeout 5m
kubectl -n segroup8 rollout status deployment/benefits-finance --timeout=180s
# 随后再次检查 readiness、/actuator/info commit 与一个公开接口的 401/200 预期
```

回滚应用前先确认旧版本与当前 Flyway schema 向后兼容；数据库迁移不通过删除资金表回退。

## 日志检索

按 `traceId`、`requestId`、`orderId`、`quoteId`、`paymentRequestId`、`refundRequestId` 或 `transactionId` 检索。用户标识必须掩码；金额统一两位小数并带 `currency`。
