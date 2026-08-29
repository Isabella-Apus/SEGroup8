# UC09 测试计划

## 验收目标

验证“商品风险审核”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `INT-TC09-001` | 集成/API | `microservices/risk-service/src/test/java/com/segroup8/risk/RiskApiIntegrationTest.java#submitReviewAndRejectUseTheDatabase`; `#forbiddenWordRuleIsDeterministicWithoutLlm`; `#approvalCreatesPendingOutboxDelivery`; `#rejectReasonAndSingleDecisionAreEnforced` | HTTP、数据库状态、敏感词、outbox、审核理由和单次决策一致 |
| `E2E-TC09-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-b/uc09-risk-audit.spec.ts#lists and rejects a deterministic high-risk product, then persists the decision`; `#rejects a forged ordinary-user administrator request` | 风险审核持久化和管理员权限越权均在真实栈验证，失败为非零退出码并保留原始证据 |

当前源码未发现按 UC09 标记的独立 Unit 测试；服务/API 集成测试不计入 Unit。

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC09/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC09/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-b/uc09-risk-audit.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
