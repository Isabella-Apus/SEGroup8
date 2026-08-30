# MS-04 构建与部署测试报告

## 最近本地验证

| 项目 | 结果 | 证据边界 |
|---|---|---|
| Maven 独立构建与测试 | PASS | `security-contract 5/5`，`secondhand-service 20/20` |
| 已测试 JAR 制作镜像 | PASS | 运行时 Dockerfile 只复制 `target/secondhand-service-*.jar` |
| 容器身份 | PASS | 本地镜像用户 `10001:10001` |
| 独立镜像 + MySQL API E2E | PASS | UC16-UC19 `4/4` |
| Compose 配置 | PASS | `docker compose ... config --quiet` |
| 部署脚本语法 | PASS | `bash -n .github/scripts/deploy-secondhand-k3s.sh` |
| Workflow YAML | PASS | YAML 解析通过 |
| 本机 Helm lint/template | NOT_RUN | 当前机器未安装 Helm；由 Actions `delivery-config` 执行 |
| 实际 K8s rollout | NOT_RUN | 仅 main 发布且开启生产部署变量时执行 |

## CI/CD 门禁

`Secondhand Service CI/CD` 的顺序为：Maven/真实 MySQL → 从已测试 JAR 构建候选镜像 → 独立服务 API E2E + 完整系统 Domain D E2E → Helm lint/template → main 发布 SHA 镜像并保存 digest → 共享生产锁下 Helm 原子部署 → rollout、版本、存活、就绪、公开接口冒烟。

PR/功能分支不会发布镜像或部署，因此这些 job 显示 `skipped` 是预期行为，不等同于部署成功。实际 digest、Helm revision 和 rollout 只能由 main 的生产 run 证明。
