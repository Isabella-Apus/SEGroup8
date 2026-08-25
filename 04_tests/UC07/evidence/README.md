# UC07 原始运行证据

本目录只保存 UC07 卖家商品生命周期及必要回归检查的运行证据。

| 证据 | 文件 | 用途 |
|---|---|---|
| UC07 独立日志 | `logs/uc07-maven-clean-test.log` | 证明 UC07 5/5 与 `BUILD SUCCESS` |
| Catalog 回归日志 | `logs/catalog-regression-test.log` | 证明 UC06+UC07 7/7，无既有功能回归 |
| Surefire XML | `raw-reports/TEST-com.segroup8.catalog.CatalogLifecycleApiAndE2ETest.xml` | 机器可读 UC07 原始结果 |
| Surefire 文本 | `raw-reports/com.segroup8.catalog.CatalogLifecycleApiAndE2ETest.txt` | 人工复核 UC07 统计 |
| 结果摘要 | `result-summary.json` | 看板或脚本读取的结构化结论 |
| 证据页 | `index.html` | 展示本次运行摘要 |
| 截图 | `screenshots/UC07-test-result.png` | 证据页浏览器截图 |

独立复现命令：`mvn -B -f microservices/catalog-service/pom.xml -Dtest=CatalogLifecycleApiAndE2ETest clean test`。
