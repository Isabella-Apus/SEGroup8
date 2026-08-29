# API/集成测试报告

- 最终运行时间：2026-08-29 17:55-17:56 CST
- 环境：Windows，JDK 17，Spring Boot 3.3.4，H2 MySQL mode，MySQL 8.4 Testcontainers
- 命令：`mvn -B -f microservices/pom.xml -pl identity-governance-service -am test`
- 共享 `security-contract`：5 tests，5 passed，0 failed，0 errors，0 skipped
- `identity-governance-service`：10 tests，10 passed，0 failed，0 errors，0 skipped
- 真实 MySQL migration：1 test，passed，未跳过
- 手工 OpenAPI 与运行时 Springdoc：34/34 method-path 契约一致，missing 0，extra 0

最终测试还逐一验证了全部受保护公开路径拒绝匿名请求、全部管理员路径拒绝普通用户。第一次 `clean verify` 因 Spring Bean 循环依赖失败；把 JWT verifier 和密码编码器改为静态 Bean 后重跑通过。失败没有从报告中删除或改写为成功。

原始 XML/TXT 由 Surefire 写入 `evidence/raw-reports/surefire/`。
