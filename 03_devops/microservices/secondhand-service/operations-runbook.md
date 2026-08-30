# secondhand-service 运维手册

## 1. 本地质量门

```bash
mvn -B -f microservices/pom.xml -pl secondhand-service -am clean verify
docker build -f microservices/secondhand-service/Dockerfile -t segroup8/secondhand:local microservices
```

测试依赖本机 Docker，以 Testcontainers 启动 MySQL 8.4.6，并验证 `secondhand_app` 无权写 `order_db.order_info`。

## 2. Helm 渲染检查

```bash
helm lint deploy/helm/segroup8 \
  --set-string backend.image.repository=registry.example/segroup8/backend \
  --set-string backend.image.tag=sha-test \
  --set-string frontend.image.repository=registry.example/segroup8/frontend \
  --set-string frontend.image.tag=sha-test \
  --set secondhand.enabled=true \
  --set-string secondhand.image.repository=registry.example/segroup8/secondhand \
  --set-string secondhand.image.tag=sha-test \
  --set-file mysql.initSchema=backend/src/main/resources/schema.sql
```

## 3. Secret 准备

由仓库管理员在目标命名空间创建 `segroup8-secondhand-secret`。只检查键名，不输出值：

```bash
kubectl -n segroup8 get secret segroup8-secondhand-secret
kubectl -n segroup8 describe secret segroup8-secondhand-secret
```

必需键：

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET（或统一后的 JWT_PUBLIC_KEY）
INTERNAL_SERVICE_TOKEN
```

## 4. 部署与回滚

生产部署由 `.github/workflows/ci-cd-microservices.yml` 执行。顺序固定为：服务测试 → Domain D Compose E2E → Helm 校验 → SHA 镜像 → Helm 原子部署。

人工复核命令：

```bash
kubectl -n segroup8 rollout status deployment/segroup8-secondhand --timeout=5m
kubectl -n segroup8 get pods -l app.kubernetes.io/component=secondhand-service
helm -n segroup8 history segroup8
```

若需回滚到上一 revision：

```bash
helm -n segroup8 rollback segroup8 PREVIOUS_REVISION --wait --timeout 5m
```

正常流水线使用 `helm upgrade --atomic`，部署失败会自动回滚，不应使用可变 `latest` 标签补救。

## 5. 探针与版本

```bash
kubectl -n segroup8 port-forward service/secondhand-service 18080:8080
curl --fail http://127.0.0.1:18080/actuator/health/liveness
curl --fail http://127.0.0.1:18080/actuator/health/readiness
curl --fail http://127.0.0.1:18080/actuator/info
```

- liveness：只判断进程是否存活，不依赖 `order-service`。
- readiness：依赖本地数据库连接和启动时 Flyway 成功，不把远程订单服务纳入就绪条件。
- info：返回 `APP_VERSION`、`APP_COMMIT`、`APP_BUILD_TIME`。

## 6. 日志与定位

```bash
kubectl -n segroup8 logs deployment/segroup8-secondhand --tail=200
kubectl -n segroup8 logs deployment/segroup8-secondhand --previous
kubectl -n segroup8 describe deployment segroup8-secondhand
kubectl -n segroup8 get events --sort-by=.lastTimestamp
```

HTTP 日志统一带 `traceId`、`requestId`。业务日志定位时同时记录或检索 `productId`、`tradeType`、`tradeId`、`auctionId`、`orderBusinessKey`、`eventId`，禁止打印 JWT、内部令牌和数据库口令。

## 7. 恢复任务

- `TradeRecoveryScheduler` 周期扫描 `PENDING/RETRY` 的 `trade_order_request`。
- `AuctionSettlementScheduler` 周期扫描已结束但未结算的拍卖。
- 远程创建订单超时后，先按 `orderBusinessKey` 查询，只有确认不存在才重试。
- 超过 `ORDER_MAX_ATTEMPTS` 后直接购买解除商品冻结；议价和拍卖保留可审计失败状态。
- 两个任务均可重入，多副本竞争依赖数据库 CAS 和业务唯一键，不得人工直接修改订单表。

暂停恢复任务时应先缩容服务或通过配置统一调整调度间隔，处理完成后再恢复副本并检查 `trade_order_request` 与 `outbox_event` 积压。

`outbox_event.event_status=NEW` 由全队统一的 relay/CDC 组件消费。本服务当前只保证业务数据与事件在同一事务落库；共享投递组件接入前应把 `NEW` 积压标记为待集成，不得标记为已送达。排障时只允许使用只读账号查询数量和最早创建时间，不得手工删除事件。

## 8. order-service 故障

1. 确认本服务 readiness 仍为 `UP`。
2. 检查请求是否返回处理中或暂不可用，而非生成重复订单。
3. 查询本服务数据库中的 `trade_order_request` 状态和尝试次数。
4. 恢复 order-service 后观察按 business key 查询和重试日志。
5. 验证每个 `(trade_type, trade_id)` 只有一条请求且只有一个外部订单引用。
