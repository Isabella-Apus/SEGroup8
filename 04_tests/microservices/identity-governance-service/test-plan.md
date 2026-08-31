# MS-01 测试计划

| 层级 | 范围 | 门禁 |
|---|---|---|
| 单元 | JWT claims 与共享 verifier 兼容 | 失败即 Maven 失败 |
| API | 31 个公开 method-path 成功矩阵、重复/错误凭据、29 个匿名拒绝、10 个管理员越权、29 个失效账户状态 | MockMvc + H2/Flyway |
| 集成 | UC02 地址所有权、UC03 审批+outbox、UC04 封禁、UC05 举报拉黑信用 | Controller → JWT → Service → JDBC → migration |
| MySQL | 空库 Flyway、9 张核心表，以及注册→登录→JWT→资料写读→真实落库全链路 | Testcontainers MySQL 8.4 |
| 契约 | 内部服务 Token、请求 ID、幂等键、最小用户摘要、introspection | MockMvc |
| E2E | 复用 `frontend/e2e/domain-a/uc01-uc05*.spec.ts` | 单体基线 5/5、微服务栈 5/5；均已运行 |
| 隔离 | 服务账号访问本 Schema 成功、访问 `order_db` 被拒 | 本地 Compose 脚本 |
| 部署 | Docker 错口令失败/恢复；Helm lint/template；主流水线原子发布 | 本地演练已通过，Helm/远端发布以 PR Actions 为准 |

命令：`mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify`。
