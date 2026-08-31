# catalog-shop-service 架构交付

本目录是 MS-02 的唯一架构交付位置。`catalog-shop-service` 以一个 Spring Boot 进程部署，内部按 catalog、shop、risk、behavior 划分代码与数据职责；库存属于 catalog 强一致边界。旧的四个重复原型已移除，迁移依据保留在版本化 Flyway 脚本、架构文档和 Git 历史中，不再形成可误部署的第二套服务源码。

- 服务边界与兼容策略：`service-boundary.md`
- 图源与渲染件：`service-diagram.mmd`、`service-diagram.svg`
- 契约：`openapi.yaml`
- 数据归属：`database-ownership.md`
- 跨服务失败语义：`cross-service-calls.md`
- 迁移核对：`migration-version-report.md`
- UC 追溯：`traceability.md`
- 完整交付索引：`delivery-manifest.md`
