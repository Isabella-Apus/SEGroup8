# secondhand-service 运维交付

Helm 资源统一位于 `deploy/helm/segroup8/templates/secondhand-*.yaml`，服务固定使用 `replicaCount`，不再提供二手专属 HPA。

- [operations-runbook.md](operations-runbook.md)：构建、部署、日志、探针、版本和 Order 恢复任务；
- 完整云端故障实验：`scripts/experiments/cloud-native/run_dependency_fault_experiment.sh`；
- 正式实验报告：`03_devops/cloud-native-experiments/README.md`。

Secret `segroup8-secondhand-secret` 保存数据库、JWT 和内部服务凭据；ConfigMap 保存下游地址、超时、重试和恢复任务参数。候选镜像经 UC16–UC19 API/E2E 验证后原样推送为 `secondhand:sha-<完整提交号>`，发布阶段不重新构建。
