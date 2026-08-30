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
| Workflow YAML | PASS | `actionlint` 结构检查通过 |
| 本机 Helm lint/template | PASS | 1 chart linted，HPA/Deployment 静态断言通过 |
| HPA 本地预实验 | PASS | Docker Desktop Kubernetes v1.34.3；1→3→1，峰值 Ready 3，15 VU/150s 零错误 |
| 实际 K8s rollout | NOT_RUN | 仅 main 发布且开启生产部署变量时执行 |

HPA 预实验原始证据：

- 汇总：`evidence/hpa/20260830-181116-preexperiment-summary.json`
- HPA/Pod/CPU 快照：`evidence/hpa/20260830-181116-hpa-snapshots.csv`
- k6：14,814 请求，102.34 req/s，平均 148.99 ms，P95 303.16 ms，HTTP/业务/服务端错误率均为 0。
- Kubernetes：初始 1、峰值 3、峰值 Ready 3、停压后最终 1；单 Pod 采样峰值 551m CPU、208 MiB。

这是 Day8 扩缩容预实验，只证明 HPA 链路可用；不替代 Day9 同机、同数据、同脚本的三轮正式性能对比。

## CI/CD 门禁

`Secondhand Service CI/CD` 的顺序为：Maven/真实 MySQL → 从已测试 JAR 构建候选镜像 → 独立服务 API E2E +
完整系统 Domain D E2E → Helm lint/template → main 发布 SHA 镜像并保存 digest → 共享生产锁下 Helm 原子部署 →
rollout、版本、存活、就绪、公开接口冒烟。

PR/功能分支不会发布镜像或部署，因此这些 job 显示 `skipped` 是预期行为，不等同于部署成功。当前提交仍需等待
远程 Actions。ACR digest、生产 Helm revision、真实 rollout 和错误镜像回滚演示只能由 main 的全队部署环境生成，
不能用本地 HPA 预实验替代。
