# secondhand-service 运维交付

本目录对应 `MS-04`，描述 `secondhand-service` 的部署、回滚、探针、日志、恢复任务和故障演练。Helm 资源统一位于 `deploy/helm/segroup8/templates/secondhand-*.yaml`，不维护第二份部署清单。

## 文件

- [operations-runbook.md](operations-runbook.md)：构建、部署、观察和恢复操作。
- [deployment-failure-drill.md](deployment-failure-drill.md)：错误镜像参数、诊断和原子回滚演练。
- [run-hpa-preexperiment.ps1](run-hpa-preexperiment.ps1)：创建隔离测试环境，执行 k6 并保存 HPA 扩容、就绪和回落证据。

## 配置归属

ConfigMap 保存服务地址、超时和重试参数；Kubernetes Secret `segroup8-secondhand-secret` 保存 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 或 `JWT_PUBLIC_KEY`、`INTERNAL_SERVICE_TOKEN`。仓库中不得提交这些值。

镜像只使用不可变标签：

```text
${ACR_REGISTRY}/${ACR_NAMESPACE}/secondhand:sha-${GIT_SHA}
```

`secondhand.enabled` 和 `secondhand.autoscaling.enabled` 默认为 `false`，专用流水线完成测试、E2E 和 Helm
静态门禁后才显式启用。HPA 默认使用 1–4 副本和 70% CPU 目标。
