# MS-04 构建与部署测试报告

最近本地执行时间：2026-08-31 01:33（UTC+08:00）

## 本地验证结果

| 项目 | 结果 | 证据边界 |
|---|---|---|
| Maven 独立构建与测试 | PASS | `security-contract 5/5`，`secondhand-service 21/21`，合计 26/26 |
| MySQL 8.4.6 Flyway/权限/并发 | PASS | V1/V2 迁移、跨库写拒绝、独立拍卖并发无死锁 |
| 已测试 JAR 制作镜像 | PASS | 运行时 Dockerfile 只复制 `target/secondhand-service-*.jar` |
| 容器身份 | PASS | 本地镜像用户 `10001:10001` |
| 独立镜像 + MySQL API E2E | PASS | UC16-UC19 `4/4`，6.3 秒 |
| Compose 配置 | PASS | `docker compose ... config --quiet` |
| Workflow/部署脚本 | PASS | actionlint 与 Bash 语法检查通过 |
| Helm lint/template | PASS | HPA、Deployment、探针与 Secret/ConfigMap 静态断言通过 |
| HPA 正式本地实验 | PASS | 唯一当前镜像，Deployment 1→4→1，峰值 Ready 3，10,692 请求零错误 |
| 错误镜像与 Helm 回滚 | PASS | 隔离本地 Kubernetes；revision 3 failed，revision 4 rollback to 2 |
| 生产 K8s rollout | NOT_RUN | 仅 main 发布且共享部署环境可用时执行 |

## HPA 正式实验

- 环境：Docker Desktop Kubernetes v1.34.3、Metrics Server v0.9.0。
- 镜像：`segroup8/secondhand:hpa-20260830-234945`，由当前提交构建并导入节点，Deployment 使用 `imagePullPolicy=Never`，避免复用旧镜像。
- 配置：15 VU、150 秒、CPU 目标 70%、min 1、max 4。
- 扩缩容：初始 1、峰值 4、停压后 1；峰值 Ready 为 3/4，说明本机高负载时新 JVM 实例启动较慢，不写成 4/4 Ready。
- 压测：10,692 请求，71.564 req/s，平均 208.57 ms，P95 413.30 ms，HTTP 失败率、业务失败率、服务端错误率均为 0。
- 证据：`evidence/hpa/20260830-234945-formal-summary.json`、快照 CSV、k6 JSON/console 与 Kubernetes 资源日志。

## 部署失败与回滚

隔离命名空间先部署唯一基线镜像，再升级到不存在的镜像标签。Helm 原子升级按预期以退出码 1 失败，
`helm history` 记录 revision 3 为 `failed`、revision 4 为 `Rollback to 2`。回滚后镜像恢复，
readiness 为 `UP`，`/actuator/info` 返回基线版本和验证提交。证据位于
`evidence/deployment-drill/20260830-233919-*`。

## CI/CD 门禁

`Secondhand Service CI/CD` 的顺序为：Maven/真实 MySQL → 从已测试 JAR 构建候选镜像 → 独立服务 API E2E +
完整系统 Domain D E2E → Helm lint/template → main 发布 SHA 镜像并保存 digest → 共享生产锁下 Helm 原子部署 →
rollout、版本、存活、就绪、公开接口冒烟。

PR/功能分支不会发布镜像或部署，因此发布 job 显示 `skipped` 是预期行为，不等同于部署成功。当前提交仍需
远程 Actions；ACR digest、生产 Helm revision 和真实共享集群 rollout 只能由 main 环境生成。
