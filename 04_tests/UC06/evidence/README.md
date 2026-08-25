# UC06 原始运行证据

本目录只保存 UC06 商品搜索筛选与详情的测试运行证据。

| 证据 | 文件 | 用途 |
|---|---|---|
| 完整运行日志 | `logs/maven-clean-test.log` | 证明命令、测试统计及 `BUILD SUCCESS` |
| Surefire XML | `raw-reports/TEST-com.segroup8.catalog.CatalogApiAndE2ETest.xml` | 机器可读的原始测试结果 |
| Surefire 文本 | `raw-reports/com.segroup8.catalog.CatalogApiAndE2ETest.txt` | 人工复核测试统计 |
| 结果摘要 | `result-summary.json` | 看板或脚本读取的结构化结论 |
| 证据页 | `index.html` | 展示本次运行摘要 |
| 截图 | `screenshots/UC06-test-result.png` | 证据页的浏览器截图 |

复现命令：`mvn -B -f microservices/pom.xml clean test`。
