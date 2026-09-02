# secondhand-service 运维手册

## 构建与独立验收

```bash
mvn -B -f microservices/pom.xml -pl secondhand-service -am clean verify
docker compose -f microservices/secondhand-service/compose.acceptance.yml up -d --build
```

独立 API/E2E、Domain D 完整系统 E2E、候选镜像和 Helm 校验由 `Secondhand Service CI/CD` 执行。主分支发布 `secondhand:sha-<完整提交号>`，保存 registry digest，并使用 `helm upgrade --atomic --cleanup-on-fail --wait` 串行更新共享 release。

## 日志、探针与版本

```bash
kubectl -n segroup8 get deployment,pod,service -l app.kubernetes.io/component=secondhand-service
kubectl -n segroup8 logs deployment/segroup8-secondhand --tail=200
kubectl -n segroup8 logs deployment/segroup8-secondhand --previous --tail=200

SECONDHAND_IP=$(kubectl -n segroup8 get service secondhand-service -o jsonpath='{.spec.clusterIP}')
curl -fsS "http://$SECONDHAND_IP:8080/actuator/health/liveness"
curl -fsS "http://$SECONDHAND_IP:8080/actuator/health/readiness"
curl -fsS "http://$SECONDHAND_IP:8080/actuator/info"
```

日志使用单行 JSON，并以 `traceId`、`requestId`、`eventId` 和业务键关联请求；不记录 JWT、内部令牌、密码或完整地址。生产只公开批准的 Actuator 端点，不公开 Flyway、env、beans 或 heapdump。

## Order 不可用与恢复

Order 不可用时，二手服务 readiness 保持 `UP`；二手列表、详情和发布不应被拖垮。需要建单的直购/拍卖结算写入 `trade_order_request` 并返回 HTTP 202/`RETRY`。恢复任务使用数据库 `CURRENT_TIMESTAMP` 判断到期时间，并以同一 business key 查询/创建，避免跨时区延迟和重复订单。

```sql
SELECT id, product_id, order_business_key, request_status,
       attempts, next_retry_at, last_error
FROM trade_order_request
WHERE request_status IN ('PENDING','RETRY')
ORDER BY next_retry_at;
```

正式云实验只暂停 `segroup8-cloud-exp-*` 中的 Order；结果见 `04_tests/cloud-native-experiments/20260902-order-fault-b622e6bb/`。生产 Order 停止时，二手非建单能力可用，建单能力等待恢复。

## 部署失败定位

1. 查看失败 Actions job 和部署诊断 artifact；
2. 用 `helm status/history` 判断升级失败与原子回滚；
3. 用 `kubectl get/describe/events` 区分镜像、Secret、调度和探针；
4. 查看当前与 `--previous` JSON 日志并按 requestId/traceId 串联；
5. 用 `/actuator/info` 核对运行 commit；
6. 修复后重新部署，验证 rollout、liveness、readiness、version 和公开 smoke。

完整系统的一次真实部署失败定位过程、HPA 和 Order 故障恢复结论统一见 `03_devops/cloud-native-experiments/README.md`。
