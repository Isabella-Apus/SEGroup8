# order-service

独立订单服务，覆盖 UC11-UC15、UC20。唯一入口是 `com.segroup8.order.OrderApplication`。

```bash
mvn -B -f microservices/pom.xml -pl order-service -am clean verify
docker build -f microservices/order-service/Dockerfile -t segroup8/order:local microservices
```

运行时敏感配置通过 Kubernetes Secret 注入：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`（或未来的 `JWT_PUBLIC_KEY`）、`INTERNAL_SERVICE_TOKEN`。服务地址由 ConfigMap 注入，不写公网 IP。

测试源码按交付要求位于 `src/test/{unit,api,integration,contract}`，由 build-helper 加入 Maven 测试源。真实 MySQL 权限测试在 Docker 可用时自动运行。
