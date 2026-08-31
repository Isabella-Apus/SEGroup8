# finance 故障隔离演练

## 步骤

1. 准备一个 `PENDING_PAY` 测试订单并记录 orderId/paymentRequestId。
2. `kubectl -n segroup8 scale deploy/benefits-finance-service --replicas=0`（或注入超过 2s 的延迟）。
3. 使用固定 `Idempotency-Key` 调用 pay，预期 HTTP 503、代码 `PAYMENT_TEMPORARILY_UNAVAILABLE`，订单保持 `PAYMENT_PENDING`。
4. 同时调用 order detail/list，预期 200；liveness 仍 UP，readiness 仅由 order_db 决定。
5. 检查同一 requestId 的超时日志和 `order_saga.RESULT_PENDING`，确认没有第二次 debit 写调用。
6. 恢复 finance，使用相同幂等键重试/运行恢复任务；先查询原 paymentRequestId，成功后进入 `PENDING_SHIP`。

## 恢复

```bash
kubectl -n segroup8 scale deploy/benefits-finance-service --replicas=1
kubectl -n segroup8 rollout status deploy/benefits-finance-service --timeout=5m
```

现场执行后把命令输出、时间线和截图放入 `04_tests/microservices/order-service/evidence/`；当前仓库只提交可重复演练步骤和自动契约测试，不伪造集群结果。
