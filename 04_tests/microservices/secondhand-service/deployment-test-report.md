# MS-04 构建与部署测试报告

## 本地已验证

| 项目 | 结果 | 证据 |
|---|---|---|
| Maven 独立构建 | PASS | `secondhand-service` 16/16，安全契约 5/5 |
| Docker 镜像构建 | PASS | 本地镜像 `segroup8/secondhand:local`，ID `sha256:ea41b838b5a3...` |
| 容器身份 | PASS | UID `10001`，镜像配置用户 `appuser` |
| 可执行制品 | PASS | `/app.jar` 可读 |
| Helm lint | PASS | 1 chart linted，0 chart failed |
| Helm template | PASS | ConfigMap、Service、Deployment 成功渲染 |
| 探针配置 | PASS | liveness/readiness 分别指向 Actuator probe |
| 不可变镜像策略 | PASS（配置） | workflow 使用 `sha-${GITHUB_SHA}`，Helm 默认 `IfNotPresent` |

## 尚需部署环境验证

- GitHub Actions 成功运行链接及失败门禁截图。
- ACR 推送后的镜像 digest。
- Kubernetes 实际 rollout、探针响应、Helm revision。
- 错误启动参数演练及 `helm rollback` 结果。

这些项目不能在没有仓库 Actions 运行和 Kubernetes 访问权限的本地环境中伪造，PR 推送后按 `03_devops/microservices/secondhand-service/deployment-failure-drill.md` 补录。
