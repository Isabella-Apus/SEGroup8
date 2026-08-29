# UC08 测试计划

## 验收目标

验证“店铺查看、设置和装修”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `INT-TC08-001` | 集成/API | `microservices/shop-service/src/test/java/com/segroup8/shop/ShopApiIntegrationTest.java#viewSettingsAndDecorationUseTheDatabase`; `#decorationRuleRejectsUnknownTemplate`; `#closedShopAndUnknownSellerAreRejected`; `#malformedAndOversizedDecorationAreRejected` | HTTP、数据库状态、装修模板、店铺状态、卖家归属和输入边界一致 |
| `E2E-TC08-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-b/uc08-shop.spec.ts#publishes decoration, reloads it and confirms the persisted JSON`; `#keeps shop maintenance inaccessible to a different role` | 装修持久化、刷新回读和角色越权均在真实栈验证，失败为非零退出码并保留原始证据 |

当前源码未发现按 UC08 标记的独立 Unit 测试；服务/API 集成测试不计入 Unit。

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC08/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC08/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-b/uc08-shop.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
