# UC06 测试计划

## 验收目标

验证“商品列表、搜索筛选和详情”的主成功流程、至少一个异常或权限分支，以及页面、接口和数据库结果的一致性。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 入口 | 通过条件 |
|---|---|---|---|
| `INT-TC06-001` | 集成/API | `microservices/catalog-service/src/test/java/com/segroup8/catalog/CatalogApiIntegrationTest.java#combinedSearchAndPublicDetailUseTheDatabase`; `#filtersSortsEmptyAndExceptionPaths` | HTTP、数据库状态、搜索筛选、空结果和异常分页一致 |
| `E2E-TC06-001` | Compose + MySQL + Playwright | `frontend/e2e/domain-b/uc06-catalog.spec.ts#searches, filters, opens matching detail and confirms persisted API fields`; `#shows a real empty result and rejects invalid pagination at the API boundary` | 完整业务链路成功，失败为非零退出码并保留原始证据 |

当前源码未发现按 UC06 标记的独立 Unit 测试，因此不保留 `UNIT-TC06-001` 占位编号；服务/API 集成测试不计入 Unit。

## 执行入口

```powershell
$env:E2E_EVIDENCE_ROOT = '04_tests/UC06/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC06/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-b/uc06-catalog.spec.ts --workers=1
```

测试源码留在框架默认目录；本计划不复制 Java 或 Playwright 源码。只有 Surefire/Playwright 机器结果、日志、截图和真实退出码可以支撑 PASS。
