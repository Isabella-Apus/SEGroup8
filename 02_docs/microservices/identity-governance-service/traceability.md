# UC01-UC05 追溯表

| 需求/UC | API/代码 | 数据 | 自动测试 | 当前结果 |
|---|---|---|---|---|
| REQ01 / UC01 | `AuthController`、`AuthenticationInterceptor` | `user` | `TokenContractUnitTest`、`AuthenticationApiTest`、既有 `uc01-auth.spec.ts` | 单元/API/E2E PASS |
| REQ02 / UC02 | `UserController`、地址事务 | `user`、`address` | `IdentityGovernanceFlowIntegrationTest#profileAndAddress...`、既有 `uc02-profile-address.spec.ts` | 集成/E2E PASS |
| REQ03 / UC03 | 申请/审核、`MerchantApproved.v1` | `merchant_application`、`outbox_event` | `#merchantApprovalUpdatesRole...`、既有 `uc03-merchant-application.spec.ts` | 集成/E2E PASS |
| REQ04 / UC04 | 管理员查询、封禁/解禁 | `user`、`admin_audit_log`、`outbox_event` | `#banUnbanChangesLogin...`、既有 `uc04-ban-unban.spec.ts` | 集成/E2E PASS |
| REQ05 / UC05 | 举报、拉黑、信用、内部 block-check | `user_report`、`user_block`、`credit_score_log` | `#reportBlockCreditAudit...`、`InternalApiContractTest`、既有 `uc05-governance.spec.ts` | 集成/契约/E2E PASS |

系统级/组件级/对象级模型继续以仓库 UC01-UC05 权威模型为准；本服务图只表达部署边界，不替代三层模型。
