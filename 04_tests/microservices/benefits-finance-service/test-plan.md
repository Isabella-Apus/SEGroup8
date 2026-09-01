# MS-05 测试计划

| 层级 | 范围 | 入口 | 阻断规则 |
|---|---|---|---|
| Unit | 金额、门槛、数量、请求字段 | `VoucherRulesTest` | 任一失败阻断 |
| API | UC21–UC23 成功、权限、参数、状态 | `PublicApiTest` | 任一失败阻断 |
| Contract | 内部服务身份、扣款查询、退款、结算、券补偿 | `InternalApiContractTest` | 任一失败阻断 |
| Integration | 余额+流水原子性、重复/并发幂等、退款恢复 | `FinanceAtomicityIntegrationTest` | 任一失败阻断 |
| MySQL | Flyway、约束、真实方言 | `MySqlContractIntegrationTest` | Docker/CI 环境必须执行且任一失败阻断；本机 MySQL 8.4.6 已 PASS |
| E2E | UC21–UC23，协作引用 UC12/UC14 | `frontend/e2e/domain-e/`、Domain C | 单体兼容回归与微服务路由 E2E 分开记录；未产生对应原始证据不得标 PASS |
| Deployment | 探针、原子 Helm、故障注入、回滚 | Runbook + CI/测试集群 | 真实集群执行后方可标 PASS |

标准命令：

```bash
mvn -B -f microservices/pom.xml -pl benefits-finance-service -am clean verify
```

旧单体 Compose 浏览器兼容回归（不等同于新微服务路由 E2E）：

```bash
cd frontend
npx playwright test e2e/domain-e/uc21-voucher-lifecycle.spec.ts e2e/domain-e/uc22-claim-checkout.spec.ts e2e/domain-e/uc23-wallet-settlement.spec.ts --workers=1
```

迁移后微服务 E2E 必须在 `benefits-finance-service` 实际运行且 gateway/order 调用已路由到该服务的环境执行；证据中需要记录服务镜像 SHA、目标 URL/命名空间和关联请求 traceId。
