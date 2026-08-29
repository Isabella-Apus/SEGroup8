# MS-01 测试计划

| 层级 | 范围 | 门禁 |
|---|---|---|
| 单元 | JWT claims 与共享 verifier 兼容 | 失败即 Maven 失败 |
| API | 注册、登录、重复/错误凭据、无 token、普通用户越权 | MockMvc + H2/Flyway |
| 集成 | UC02 地址所有权、UC03 审批+outbox、UC04 封禁、UC05 举报拉黑信用 | Controller → JWT → Service → JDBC → migration |
| MySQL | 空库 Flyway 与 9 张核心表 | Testcontainers MySQL 8.4 |
| 契约 | 内部服务 Token、请求 ID、幂等键、最小用户摘要、introspection | MockMvc |
| E2E | 复用 `frontend/e2e/domain-a/uc01-uc05*.spec.ts` | 根网关切流后运行；本分支当前 NOT_RUN |
| 隔离 | 服务账号访问本 Schema 成功、访问 `order_db` 被拒 | 本地 Compose 脚本 |

命令：`mvn -B -f microservices/pom.xml -pl identity-governance-service -am clean verify`。
