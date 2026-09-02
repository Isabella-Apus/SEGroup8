# order-service 架构交付

本目录是 MS-03 的唯一架构交付位置。`order-service` 拥有 UC11-UC15、UC20 的订单、售后、评价与物流数据，编排 catalog 库存和 finance 资金，不访问其他服务数据库。

- [服务边界与状态机](service-boundary.md)
- [服务图](service-diagram.mmd) / [SVG](service-diagram.svg)
- [改造前后代码差异](before-after-code-diff.md)
- [OpenAPI](openapi.yaml)
- [数据库归属](database-ownership.md)
- [跨服务调用](cross-service-calls.md)
- [追溯矩阵](traceability.md)
