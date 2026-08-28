# UC06 商品搜索筛选与详情测试报告

## 测试范围

验证关键词、分类、店铺、价格区间、价格排序、在售商品详情，以及空结果、非法参数和下架商品不可见场景。API 集成测试与真实浏览器 E2E 分层执行。

## 测试环境

| 项目 | 内容 |
|---|---|
| 运行时间 | 2026-08-25 16:22:43 +08:00 |
| Java | 17.0.13 |
| Spring Boot | 3.3.4 |
| 测试数据库 | H2 内存数据库（MySQL 兼容模式） |
| 执行命令 | `mvn -B -f microservices/pom.xml clean test` |

## 测试结果

| 编号 | 场景 | 自动化测试方法 | 结果 |
|---|---|---|---|
| T06-01 | 组合关键词、分类、价格区间搜索，并查看在售详情 | `combinedSearchAndPublicDetailUseTheDatabase` | 通过 |
| T06-02 | 店铺和分类筛选、价格升降序、空结果、下架详情、非法价格参数 | `filtersSortsEmptyAndExceptionPaths` | 通过 |
| T06-03 | 登录后搜索真实商品、打开详情并与持久化 API 数据核对 | `uc06-catalog.spec.ts` | 由 Domain B Browser E2E 执行 |
| T06-04 | 浏览器空结果与非法分页参数 | `uc06-catalog.spec.ts` | 由 Domain B Browser E2E 执行 |

## 汇总

| 测试数 | 通过 | 失败 | 错误 | 跳过 | 构建结果 |
|---:|---:|---:|---:|---:|---|
| 2 | 2 | 0 | 0 | 0 | BUILD SUCCESS |

## 结论

`CatalogApiIntegrationTest` 是 Spring Boot + MockMvc + H2 的 API 集成测试，不应标注为 E2E。真实 E2E 通过 Compose 启动前端、后端和 MySQL 后运行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser
```

该命令会执行 `frontend/e2e/domain-b/uc06-catalog.spec.ts`，并把 Playwright JUnit、JSON、HTML 报告和失败诊断材料写入 `04_tests/domains/B-catalog-shop/evidence/`；CI 将这些结果作为 artifact 保存。

## 原始证据

- [测试代码](../../microservices/catalog-service/src/test/java/com/segroup8/catalog/CatalogApiIntegrationTest.java)
- [测试配置](../../microservices/catalog-service/src/test/resources/application.yml)
- [完整 Maven 日志](../../04_tests/UC06/evidence/logs/maven-clean-test.log)
- [Surefire XML 原始报告](../../04_tests/UC06/evidence/raw-reports/TEST-com.segroup8.catalog.CatalogApiAndE2ETest.xml)
- [Surefire 文本报告](../../04_tests/UC06/evidence/raw-reports/com.segroup8.catalog.CatalogApiAndE2ETest.txt)
- [UC06 运行结果截图](../../04_tests/UC06/evidence/screenshots/UC06-test-result.png)
