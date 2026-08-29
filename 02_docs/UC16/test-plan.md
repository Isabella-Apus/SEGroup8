# UC16 测试计划

## 测试目标

验证二手商品发布与管理在真实 Controller、Service 和数据库路径中的分类、持久化、所有权、状态机和输入边界，并通过真实 Compose + Playwright 验证页面操作。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 场景 | 核心断言 |
|---|---|---|---|
| `UNIT-TC16-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/SecondhandProductServiceImplTest.java#pagePublicProducts_shouldThrowWhenPriceRangeInvalid`; `#createSellerProduct_shouldThrowWhenOriginPriceLowerThanSalePrice`; `#pageSellerProducts_shouldFilterByCurrentUser` | 价格、字段边界和本人列表隔离 |
| `MVC-TC16-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/SecondhandProductControllerUc16WebMvcTest.java#sellerCreate_shouldValidateRequestAndReturnProduct`; `#publicList_shouldRecordKeywordAndDelegateQuery` | 发布请求校验和公开列表契约 |
| `INT-TC16-001` | HTTP + DB Integration | `backend/src/test/java/com/segroup8/platform/integration/SecondhandProductManagementIntegrationTest.java#realCategoryCreateEditShelfAndReload_arePersistedAcrossHttpAndDatabase`；真实分类树、发布、编辑、下架、上架和重查 | HTTP 响应、商品表和公开可见性一致 |
| `INT-TC16-002` | HTTP + DB Integration | `#nonOwnerCannotEditShelfOrDelete_andProductRemainsUnchanged`；非本人编辑、上下架和删除 | 业务码 403，商品数据不变 |
| `INT-TC16-003` | HTTP + DB Integration | `#soldProductCannotBeEditedRelistedOrDeleted`；已售商品编辑、上架和删除 | 全部拒绝，状态保持 `3` |
| `INT-TC16-004` | HTTP + DB Integration | `#ownerDeleteRemovesProductFromSellerPublicAndDetailQueries`；本人删除下架商品 | 数据库、详情和列表均不可见 |
| `INT-TC16-005` | Validation + DB | `#invalidNameImagesCategoryConditionNegotiableAndPrices_doNotWriteProducts`；名称、图片数量、分类、成色、议价标志和价格边界 | 11 个非法请求均拒绝且无写入 |
| `E2E-TC16-001` | Playwright + MySQL | `frontend/e2e/domain-d/uc16-product-management.spec.ts#persists publish, edit, shelf changes and delete while rejecting a non-owner`；发布、编辑、下架、上架、刷新、第三方越权、删除 | 页面和 API 状态持久一致，越权拒绝，删除后消失 |

## 本地验证

H2 快速 Integration：

```powershell
Push-Location backend
mvn -B --no-transfer-progress '-Dtest=SecondhandProductManagementIntegrationTest' test
Pop-Location
```

同一测试类连接 Compose MySQL：

```powershell
docker compose up -d database
Push-Location backend
mvn -B --no-transfer-progress '-Dtest=SecondhandProductManagementIntegrationTest' `
  '-Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver' `
  '-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3307/segroup8_platform' `
  '-Dspring.datasource.username=segroup8' `
  '-Dspring.datasource.password=segroup8_dev_password' `
  '-Dspring.sql.init.mode=never' test
Pop-Location
```

真实浏览器统一入口：

```powershell
$env:DOMAIN_D_SUITE = 'UC16'
$env:E2E_EVIDENCE_ROOT = '04_tests/UC16/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC16/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-d/uc16-product-management.spec.ts --workers=1
```

命令失败必须保留非零退出码。只有 Surefire、Playwright、截图和 Compose 日志可作为通过证据。
