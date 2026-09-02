# UC08 店铺查看、设置和装修测试报告

## 测试范围

验证开放店铺查看、卖家本人店铺查询、设置、装修、关店、卖家隔离、装修规则，以及商品目录故障时的降级行为。Spring Boot + MockMvc API 集成测试与真实浏览器 E2E 分层执行。

## 测试环境

| 项目 | 内容 |
|---|---|
| 运行时间 | 2026-08-26 09:12:17 +08:00 |
| Java | 17.0.13 |
| Spring Boot | 3.3.4 |
| 测试数据库 | H2 内存数据库（MySQL 兼容模式） |
| 独立命令 | `mvn -B -f microservices/shop-service/pom.xml clean test` |
| 回归命令 | `mvn -B -f microservices/pom.xml clean test` |

## UC08 独立测试结果

| 编号 | 场景 | 自动化测试方法 | 结果 |
|---|---|---|---|
| T08-01 | 公开查看、卖家设置、装修和目录故障降级 | `t0801_publicViewSettingsDecorationAndCatalogFallback` | 通过 |
| T08-02 | 非法装修模板 | `t0802_decorationRuleRejectsUnknownTemplate` | 通过 |
| T08-03 | 关闭店铺不可公开访问及卖家隔离 | `t0803_closedShopIsNotPublicAndUnknownSellerCannotMaintainIt` | 通过 |
| T08-04 | 非法、非对象和超长装修内容 | `t0804_rejectsMalformedNonObjectAndOversizedDecoration` | 通过 |

## 汇总

| 执行范围 | 测试数 | 通过 | 失败 | 错误 | 跳过 | 构建结果 |
|---|---:|---:|---:|---:|---:|---|
| UC08 独立测试 | 4 | 4 | 0 | 0 | 0 | BUILD SUCCESS |
| UC06+UC08 回归测试 | 6 | 6 | 0 | 0 | 0 | BUILD SUCCESS |

## 真实浏览器 E2E

`ShopApiIntegrationTest` 是 API 集成测试，不应标注为浏览器 E2E。真实 Compose/MySQL 环境执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 04_tests/domains/B-catalog-shop/run-domain-b.ps1 -Browser
```

`frontend/e2e/domain-b/uc08-shop.spec.ts` 先清空卖家装修数据，再通过前端添加并发布 Banner，刷新页面并调用卖家 API 验证持久化 JSON；同时验证普通用户无法访问或写入店铺装修。2026-08-27 本地 Domain B 浏览器套件结果为 7 passed、0 failed。CI 上传的 `domain-b-playwright-reports` artifact 保存 JUnit、JSON、HTML 报告和失败诊断材料。

## 结论

UC08 的需求、设计、实现、测试和运行证据已建立完整追溯；四个 UC08 场景均可重复运行，本次全部通过，且现有 UC06 测试未发生回归。

当前 `main@b622e6bb` 已由 Catalog-Shop 流水线 33526387391 和完整系统流水线 33526387696 在合并后的微服务与真实浏览器环境复验通过。

## 原始证据

- [UC08 API 集成测试代码](../../microservices/shop-service/src/test/java/com/segroup8/shop/ShopApiIntegrationTest.java)
- [测试配置](../../microservices/shop-service/src/test/resources/application.yml)
- [Surefire XML 原始报告](../../04_tests/UC08/evidence/raw-reports/TEST-com.segroup8.shop.ShopApiAndE2ETest.xml)
- [UC08 运行结果截图](../../04_tests/UC08/evidence/screenshots/UC08-test-result.png)
