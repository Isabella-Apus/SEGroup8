# catalog-shop-service 架构交付

本目录是 MS-02 的唯一架构交付位置。`catalog-shop-service` 以一个 Spring Boot 进程部署，内部按 catalog、shop、risk、behavior 划分代码与数据职责；库存属于 catalog 强一致边界。原 `catalog-service`、`shop-service`、`risk-service`、`behavior-service` 仅保留为迁移参照，不进入目标 Deployment。

- 服务边界与兼容策略：`service-boundary.md`
- 图源与渲染件：`service-diagram.mmd`、`service-diagram.svg`
- 契约：`openapi.yaml`
- 数据归属：`database-ownership.md`
- 跨服务失败语义：`cross-service-calls.md`
- 迁移核对：`migration-version-report.md`
- UC 追溯：`traceability.md`
- 完整交付索引：`delivery-manifest.md`
