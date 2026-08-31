# API/集成测试报告

- 最终本地运行时间：2026-08-30 18:09 CST
- 环境：Windows、JDK 17、Spring Boot 3.3.4、H2 MySQL mode、Docker/Testcontainers MySQL 8.4
- 命令：`mvn -B --no-transfer-progress -f microservices/pom.xml -pl identity-governance-service -am clean verify`
- `security-contract`：5 tests，5 passed，0 failed/errors/skipped
- `identity-governance-service`：16 tests，16 passed，0 failed/errors/skipped
- 真实 MySQL：2 tests，2 passed，0 skipped；其中一条完整经过 Controller、JWT、Service、JDBC 和 Flyway Schema
- OpenAPI 与运行时 Springdoc：34/34 method-path 一致，missing 0，extra 0
- 公开接口：31/31 有成功断言；29/29 受保护接口有匿名和失效账户状态断言；10/10 管理接口有普通用户越权断言
- Actuator：链接中不暴露 `flyway`，直接访问 `/actuator/flyway` 返回 404

逐接口映射见 `02_docs/microservices/identity-governance-service/service-api-list.md`。仓库保留历史 XML/TXT；独立流水线上传本次 `target/surefire-reports`，不再用旧仓库文件冒充本次报告。

本轮第一次沙箱内 `clean verify` 仍因 Windows `Access is denied` 导致 `security-contract` 测试编译读不到主类；相同命令在沙箱外连续两次通过，因此归类为执行环境权限故障，不归类为代码失败。历史 Spring Bean 循环依赖失败继续保留在 `failed-gates.log`。
