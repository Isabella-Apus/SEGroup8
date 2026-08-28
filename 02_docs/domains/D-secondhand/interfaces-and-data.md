# Domain D 接口与数据边界

当前权威接口和表归属分别见：

- [服务 API 清单](../../architecture/service-api-catalog.md)
- [数据库表归属](../../architecture/database-ownership.md)
- [微服务边界](../../architecture/microservice-boundaries.md)

二手域只管理二手商品、议价和拍卖事实；成交后通过幂等接口请求订单域创建订单，不直接写其他服务的业务表。当前代码仍有单体内调用，目标隔离状态为 `NOT_IMPLEMENTED`。
