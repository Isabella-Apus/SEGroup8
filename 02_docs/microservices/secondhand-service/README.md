# secondhand-service 架构交付

`secondhand-service` 是 MS-04 的独立服务，负责 UC16-UC19，并通过订单内部契约协作覆盖 UC20。服务决定二手交易的商品、买家和成交价，但不实现支付、物流、收货或订单状态机。

## 交付索引

- [服务边界](service-boundary.md)
- [服务图](service-diagram.mmd) / [SVG](service-diagram.svg)
- [改造前后代码差异](before-after-code-diff.md)
- [OpenAPI](openapi.yaml)
- [数据库归属](database-ownership.md)
- [跨服务调用与恢复](cross-service-calls.md)
- [需求追溯](traceability.md)

## 本地验证

```bash
mvn -B -f microservices/pom.xml -pl secondhand-service -am clean verify
```

运行时只需要 `secondhand_db`、JWT 密钥和内部服务凭证。`order-service` 故障不会影响 readiness；成交请求会保存在 `trade_order_request` 中并由恢复任务重试。
