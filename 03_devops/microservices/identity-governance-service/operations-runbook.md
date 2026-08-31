# 运维手册

## 运行参数

| 变量 | 必填 | 说明 |
|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 是 | 仅允许连接 `identity_governance_db` |
| `JWT_SECRET` | 是 | 至少 32 字节，与业务服务本地验签配置一致 |
| `INTERNAL_SERVICE_TOKEN` | 是 | 仅内部 API 使用 |
| `BOOTSTRAP_ADMIN_PASSWORD` | 本地演示 | 为空则不创建管理员 |
| `APP_VERSION` / `APP_COMMIT` / `APP_BUILD_TIME` | 建议 | `/actuator/info` 版本证据 |

## 诊断顺序

1. `docker compose ... ps` 查看容器和 health 状态。
2. 检查 `/actuator/health/readiness`；数据库失败时返回 DOWN。
3. `docker compose ... logs --no-color identity-governance-service` 检查 Flyway、连接和请求 ID。
4. `docker compose ... exec identity-mysql mysql ...` 核对 `flyway_schema_history`。
5. 用 `X-Request-Id` 关联 JSON 日志，禁止打印 Authorization/密码/Token。

## 回滚

镜像必须使用 `sha-<full-git-sha>`。本地回滚修改 Compose 镜像 tag 后重新 `up -d`；K3s 使用 `helm rollback segroup8 <revision> --wait`，正常流水线则由 `--atomic` 在失败时自动回滚。数据库迁移遵循前向修复，不自动删除已应用版本。变更前备份 `identity_governance_db`，恢复时记录版本、时间、操作者和验证结果。

## K3s 运行对象

- Deployment：`segroup8-identity-governance`
- ClusterIP Service：`identity-governance-service:8091`
- Secret：`identity-governance-secret`
- 探针：`/actuator/health/liveness`、`/actuator/health/readiness`
- 版本：`/actuator/info`
- HPA：模板已交付但 `identityGovernance.autoscaling.enabled=false`；只在后续扩缩容实验时启用。

`identity-governance-secret` 必须包含 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`、`INTERNAL_SERVICE_TOKEN` 和 `BOOTSTRAP_ADMIN_PASSWORD`。数据库管理员需一次性创建 `identity_governance_db` 与只访问该 Schema 的账号；Flyway 负责之后的表结构版本，不允许给服务账号跨 Schema 权限。
