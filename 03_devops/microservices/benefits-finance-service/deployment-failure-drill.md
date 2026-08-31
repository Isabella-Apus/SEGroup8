# 部署与数据库故障演练

## 目标

证明服务不可用或数据库连接失败时，订单支付失败关闭，不改变订单、余额和流水；恢复后使用相同 `paymentRequestId` 不会重复扣款。

## 演练 A：停止服务

```bash
kubectl -n segroup8 scale deployment/benefits-finance --replicas=0
```

从 order-service 发起支付，预期得到受控 `FINANCE_UNAVAILABLE`，订单保持待支付。保存 order 与 gateway 日志、Pod/Endpoint 状态和支付前余额。

恢复：

```bash
kubectl -n segroup8 scale deployment/benefits-finance --replicas=2
kubectl -n segroup8 rollout status deployment/benefits-finance
```

## 演练 B：数据库连接失败

临时将测试命名空间中的数据库 Secret 指向不可达端口并重启 Deployment。预期 readiness 为 DOWN、liveness 仍 UP，Pod 不接收流量。恢复 Secret 后滚动重启。

## 核验

1. 查询同一 `paymentRequestId`，若 `COMPLETED` 则订单使用原结果；若不存在才允许原 ID 重试。
2. 执行对账 SQL，余额变化必须等于流水和。
3. 保存 `kubectl describe pod`、events、脱敏日志、Helm history 和恢复后响应。

真实集群执行前报告标记 `PENDING`；不得用示例输出冒充演练证据。
