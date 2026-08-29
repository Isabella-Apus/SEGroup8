# 容器与部署测试报告

| 检查 | 状态 | 说明 |
|---|---|---|
| Boot JAR package | PASS | Maven package/verify 生成可执行 JAR |
| Dockerfile | PASS | 非 root 用户、端口 8091、readiness healthcheck；镜像构建成功 |
| Compose config/build/up | PASS | 2026-08-29 本地构建并启动，服务/MySQL 均 healthy |
| liveness/readiness/info | PASS | `UP` / `UP` / `local-validation` |
| 注册登录 smoke | PASS | 注册 `code=0`，登录 `code=0`、角色 `USER` |
| 数据库权限拒绝 | PASS | 自有 Schema 查询成功；跨查 `order_db.order_info` 返回 MySQL 1142 |
| GitHub Actions | CONFIGURED / NOT_RUN | 分支未 push，无远端 run |
| Kubernetes/Helm/HPA | OUT_OF_SCOPE | 用户明确暂不做云原生 |

第一次隔离检查的业务断言已通过，但脚本未复位预期 MySQL 拒绝产生的退出码；补充 `exit 0` 后重跑为进程退出码 0。另一次重跑漏设 Compose 必填变量，按原样记录为脚本调用失败；补齐环境变量后最终通过。任何后续失败都必须保留日志和退出码，不能用 Dockerfile 存在替代运行证据。

镜像检查：`USER=app`，healthcheck 指向 `/actuator/health/readiness`，本地镜像大小 140,906,870 bytes。验收后已停止并移除临时容器/网络，保留 `identity-governance-service_identity_mysql_data` volume 以便复查。
