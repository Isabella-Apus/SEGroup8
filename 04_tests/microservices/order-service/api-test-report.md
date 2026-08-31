# API 测试报告

- 最终执行命令：`mvn -B -f microservices/pom.xml -pl order-service -am clean verify`。
- 时间：2026-08-30 11:02:03 +08:00。
- 结果：order-service 9 passed，0 failed，1 skipped；另有 security-contract 5 passed。跳过项为 Docker 不可用时的真实 MySQL 权限测试。
- 已覆盖：统一响应包、前端兼容状态、未认证拒绝、创建与重复创建、买家/卖家列表详情、越权拒绝、支付、取消、提醒发货、发货/收货/完成幂等、物流兼容路径、逐项评价/追评/卖家回复及兼容别名、二手 businessKey 去重、内部 token、退款申请/批准/拒绝、管理员根路径/详情兼容路由、售后日志与批量关闭。
- 真实 HTTP 契约：catalog 预留、finance 报价/扣款/退款/结算/券释放的路径、token、幂等键与响应解析；OpenAPI YAML 解析和全部路径参数声明。

原始 Surefire XML/TXT 已同步到 `04_tests/microservices/order-service/evidence/raw-reports/`；构建目录位于 `microservices/order-service/target/surefire-reports/`。
