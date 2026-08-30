# MS-03 测试计划

| 层级 | 范围 | 自动化 |
|---|---|---|
| unit | 状态机、非法转移、金额/幂等纯规则 | `OrderStateTest` |
| API | 全公开主流程、鉴权、所有权、管理员、内部 token | `OrderApiTest` |
| contract | catalog 快照、finance 成功/失败/未知结果、secondhand businessKey、Controller/运行时 OpenAPI/评审 YAML 三方一致 | mock consumer contract + `PaymentFailureContractTest` + `OpenApiContractTest` |
| integration | Flyway 持久化、乐观锁、outbox/Saga、MySQL 跨库拒绝 | H2 日常 + `CrossSchemaPermissionTest`（Docker） |
| E2E | 候选镜像独立 API UC11-UC15/UC20，加共享全栈原 spec 回归 | `frontend/e2e/microservices/order-service-api.spec.ts`、`domain-c/`、`domain-d/uc20-fulfillment.spec.ts` |
| non-functional | readiness/liveness、结构化日志、单一镜像制品链、可下载部署诊断、故障隔离、3x k6、Helm atomic | CI/集群阶段 |

退出标准：Maven verify 通过、真实 MySQL 权限测试通过、6 条 E2E 通过、finance 故障演练通过、两版本各 3 次性能结果齐备。
