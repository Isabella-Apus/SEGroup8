# 容器与部署测试报告

| 检查 | 状态 | 说明 |
|---|---|---|
| Boot JAR package | PASS | Maven package/verify 生成可执行 JAR |
| Dockerfile | PASS | 非 root 用户、端口 8091、readiness healthcheck；镜像构建成功 |
| Compose config/build/up | PASS | 2026-08-29 本地构建并启动，服务/MySQL 均 healthy |
| liveness/readiness/info | PASS | `UP` / `UP` / `local-validation` |
| 注册登录 smoke | PASS | 注册 `code=0`，登录 `code=0`、角色 `USER` |
| 数据库权限拒绝 | PASS | 自有 Schema 查询成功；跨查 `order_db.order_info` 返回 MySQL 1142 |
| E2E frontend profile | PASS | 既有 `frontend/dist`，Nginx 8089 代理本服务 8091 |
| 错口令失败/恢复演练 | PASS | 服务 Exited(1)、readiness 不可达、SQLState 28000/1045；恢复后 healthy/UP/smoke PASS |
| Helm Deployment/Service | CONFIGURED / CI_PASS | 独立服务 run `33297661588` 已通过 Helm lint/template；实际集群 rollout 仍未运行 |
| GitHub Actions PR 门禁 | PASS | 独立服务 run `33297661588`、完整系统 run `33297661706` 均为 success |
| ACR/K3s | CONFIGURED / NOT_RUN | `main` 推送不可变镜像并 `--atomic --wait` 部署；PR run 中 publish/deploy 按设计跳过 |
| HPA 静态配置 | CONFIGURED | `autoscaling/v2`、CPU target 与副本范围已配置，并在启用形态下做 Helm 静态渲染；不等同于运行实验 |
| HPA 扩缩容实验 | NOT_RUN | HPA 模板已交付但默认关闭，按用户要求暂不执行实验 |
| 依赖故障处理实验 | NOT_RUN | 按用户要求暂不执行停止/延迟依赖服务实验 |

第一次隔离检查的业务断言已通过，但脚本未复位预期 MySQL 拒绝产生的退出码；补充 `exit 0` 后重跑为进程退出码 0。另一次重跑漏设 Compose 必填变量，按原样记录为脚本调用失败；补齐环境变量后最终通过。任何后续失败都必须保留日志和退出码，不能用 Dockerfile 存在替代运行证据。

镜像检查：`USER=app`，healthcheck 指向 `/actuator/health/readiness`，本地镜像大小 140,906,870 bytes。验收后已停止并移除临时容器/网络，保留 `identity-governance-service_identity_mysql_data` volume 以便复查。

E2E 使用独立项目名 `identity-governance-e2e` 和新测试 volume；最终 5/5 后已执行 `down -v`，只删除该临时项目的容器、网络和测试 volume，不影响上面的常规验证 volume。
