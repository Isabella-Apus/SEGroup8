# 部署失败排查演练

## 已执行的本地容器演练

2026-08-30 使用 `compose.local.yml + compose.failure-drill.yml` 执行了一次错误数据库口令演练。MySQL 容器保持 `healthy`，身份服务使用故意错误的 `DB_PASSWORD` 后以退出码 1 停止，readiness 地址无法连接。日志首先出现 Hikari/Flyway 连接失败，随后给出 `SQL State 28000`、MySQL `Error Code 1045` 和 `Access denied for user 'identity_governance_app'`。

这组现象说明数据库进程和网络已经可达，失败点是服务凭据与数据库账号不一致，而不是“Pod 没启动”或“数据库宕机”。恢复正确口令并强制重建服务容器后，容器转为 `healthy`，liveness/readiness 都为 `UP`，`/actuator/info` 返回 `failure-drill-recovered`，注册和登录 smoke 的业务码均为 0。关键原始摘录保存在 `04_tests/microservices/identity-governance-service/evidence/logs/deployment-failure-drill.log`。

## K3s/Helm 中同一故障如何定位

主流水线使用 `helm upgrade --install --atomic --wait`。如果 `identity-governance-secret` 中数据库口令错误，新 Pod 的 readiness 不会通过，Helm 等待超时后自动回滚到上一 revision。远端脚本的退出陷阱会把以下信息写入 Actions 的 `identity-governance-deployment-<sha>` artifact：

1. `helm status` 和 `helm history`：确认失败 revision 与自动回滚结果。
2. `kubectl get pods,service,ingress -o wide`：确认 Pod 状态、重启次数和调度节点。
3. `kubectl describe deployment/segroup8-identity-governance`：查看探针失败和滚动更新事件。
4. `kubectl logs deployment/segroup8-identity-governance --tail=200`：定位 SQLState 28000 / 1045。
5. 修复 Secret 后重新触发主流水线；流水线再次检查 rollout、liveness、readiness、版本和公开登录失败 smoke。

本地演练已经执行；K3s 错密钥和 `--atomic` 回滚的远端现场演练必须由 PR 合并后的实际 Actions run 证明，在此之前不得写成 `PASS`。
