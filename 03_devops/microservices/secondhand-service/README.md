# secondhand-service 运维交付

本目录对应 `MS-04`，描述 `secondhand-service` 的部署、回滚、探针、日志、恢复任务和故障演练。Helm
资源统一位于 `deploy/helm/segroup8/templates/secondhand-*.yaml`，不维护第二份部署清单。

## 文件

- [operations-runbook.md](operations-runbook.md)：构建、部署、观察、故障恢复和实验命令。
- [deployment-failure-drill.md](deployment-failure-drill.md)：本地隔离 Kubernetes 错误镜像、诊断和 Helm 回滚实测。
- [run-hpa-preexperiment.ps1](run-hpa-preexperiment.ps1)：构建唯一镜像，创建隔离环境，执行 k6 并保存 HPA 扩缩容证据。
- [run-order-dependency-drill.ps1](run-order-dependency-drill.ps1)：启动真实二手服务/MySQL，停止和恢复 HTTP 订单依赖并校验幂等恢复。

## 配置归属

ConfigMap 保存服务地址、超时和重试参数；Kubernetes Secret `segroup8-secondhand-secret` 保存
`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET` 或 `JWT_PUBLIC_KEY`、
`INTERNAL_SERVICE_TOKEN`。仓库中不得提交这些值。

镜像只使用不可变标签：

```text
${ACR_REGISTRY}/${ACR_NAMESPACE}/secondhand:sha-${GIT_SHA}
```

验证阶段从唯一已测试 JAR 构建候选镜像，并保存 JAR SHA-256、候选 Image ID 和 release metadata。
UC16-UC19 独立服务 E2E 加载并验证该镜像；合并到 `main` 后发布阶段不重新构建，只把同一候选镜像原样
换成 ACR 标签并推送，最后保存 registry digest。

`secondhand.enabled` 和 `secondhand.autoscaling.enabled` 默认为 `false`，专用流水线完成测试、E2E
和 Helm 静态门禁后才显式启用。HPA 默认使用 1-4 副本和 70% CPU 目标。
