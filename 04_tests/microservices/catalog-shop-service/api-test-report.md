# API 测试报告

- 执行时间：2026-08-31 09:47 CST
- 命令：`mvn -B -f microservices/pom.xml -pl catalog-shop-service -am clean verify`
- 结果：BUILD SUCCESS；8 tests，0 failures，0 errors，0 skipped。
- 覆盖：分类/搜索/详情、商品所有权和生命周期、店铺装修与事件幂等、审核确定性降级和决策、行为用户隔离与热词、库存预留/重复创建/重复确认。
- 原始机器报告：Maven 运行后位于 `microservices/catalog-shop-service/target/surefire-reports/`（构建产物不入库，CI 上传 artifact）。

全 reactor 的 `mvn -B -f microservices/pom.xml -Pdomain-b clean test` 也已通过；MySQL 8 专项仍需在 CI/Compose 环境采集。
