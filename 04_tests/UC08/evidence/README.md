# UC08 原始运行证据

本目录只保存 UC08 店铺查看、设置和装修及必要回归检查的运行证据。

| 证据 | 文件 | 用途 |
|---|---|---|
| UC08 独立日志 | `logs/uc08-maven-clean-test.log` | 证明 UC08 4/4 与 `BUILD SUCCESS` |
| 微服务回归日志 | `logs/microservices-regression-test.log` | 证明 UC06+UC08 6/6，无既有功能回归 |
| Surefire XML | `raw-reports/TEST-com.segroup8.shop.ShopApiAndE2ETest.xml` | 机器可读 UC08 原始结果 |
| Surefire 文本 | `raw-reports/com.segroup8.shop.ShopApiAndE2ETest.txt` | 人工复核 UC08 统计 |
| 结果摘要 | `result-summary.json` | 看板或脚本读取的结构化结论 |
| 证据页 | `index.html` | 展示本次运行摘要 |
| 截图 | `screenshots/UC08-test-result.png` | 证据页浏览器截图 |

独立复现命令：`mvn -B -f microservices/shop-service/pom.xml clean test`。
