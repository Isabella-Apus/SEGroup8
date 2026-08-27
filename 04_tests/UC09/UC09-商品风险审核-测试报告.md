# UC09 商品风险审核测试报告

- 执行时间：2026-08-26
- 命令：`mvn -B -f microservices/risk-service/pom.xml clean test`
- 环境：Java 17、Spring Boot 3.3.4、H2、MockMvc
- 结果：4 项通过，0 失败，0 错误，0 跳过，`BUILD SUCCESS`

| 测试 | 场景 | 结果 |
|---|---|---|
| T0901 | 高风险审核单创建、查询、驳回及回调补偿 | PASS |
| T0902 | 风险词命中、级别及命中项 | PASS |
| T0903 | 低风险审核通过及回调失败 outbox | PASS |
| T0904 | 驳回理由必填及禁止重复决定 | PASS |

另执行 UC06 catalog 2 项 + UC09 risk 4 项的独立服务回归，共 6 项通过。结论依据是本目录中的完整 Maven 日志、Surefire XML/TXT 和结果 JSON，而不是人工宣称。

## Real Browser E2E

`RiskApiIntegrationTest` is a Spring Boot + MockMvc API integration suite; it is not browser E2E. The real flow runs with Compose, the production frontend, backend, and MySQL:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser
```

`frontend/e2e/domain-b/uc09-risk-audit.spec.ts` creates a deterministic high-risk seller product, finds and rejects it through the administrator UI, confirms the persisted rejected decision through the administrator API, and verifies a regular user is denied the administrator endpoint. On 2026-08-27 the Domain B browser suite completed with 9 passed and 0 failed. CI retains the JUnit, JSON, HTML report, and failure diagnostics in the `domain-b-playwright-reports` artifact.
