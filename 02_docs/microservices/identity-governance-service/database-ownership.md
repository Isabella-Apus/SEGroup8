# 数据库归属与隔离

| 表 | 权限 | 说明 |
|---|---|---|
| `user` | 独占读写 | 账号、密码哈希、角色、状态、信用与权限版本 |
| `address` | 独占读写 | 用户地址；只允许 owner 修改/删除 |
| `merchant_application` | 独占读写 | 申请与审核状态 |
| `user_report` | 独占读写 | 新举报事实 |
| `report` | 只读归档 | 旧举报兼容，不双写 |
| `user_block` | 独占读写 | 双向检查所需的拉黑关系 |
| `credit_score_log` | 独占读写 | 信用变更流水 |
| `admin_audit_log` | 独占读写 | 管理审计及来源事件去重 |
| `idempotency_record`、`outbox_event` | 本 Schema 独占 | 幂等与可靠事件 |

本地 Compose 使用 MySQL 官方镜像的 `MYSQL_DATABASE`/`MYSQL_USER` 机制，只授予 `identity_governance_app` 对 `identity_governance_db.*` 的权限。初始化脚本另建 `order_db.order_info` 作为拒绝样本；验证脚本要求本 Schema 查询成功、跨 Schema 查询失败。

```powershell
$env:IDENTITY_DB_PASSWORD='<local-secret>'
./03_devops/microservices/identity-governance-service/verify-db-isolation.ps1
```

MySQL Testcontainers 已验证空库执行 `V1__identity_governance_schema.sql` 后 9 张核心活动表存在；`report` 作为归档表单独存在，不计入活动表断言。
