# secondhand-service 部署失败演练

## 目标

证明错误镜像启动参数不会替换健康版本，并能通过 Kubernetes 与 Helm 证据定位、回滚。只在测试命名空间执行，禁止修改生产 Secret。

## 前置条件

- 当前 release 健康，记录 `helm history` 中的 revision。
- 已保存当前 SHA 镜像和 `/actuator/info` 输出。
- 操作者有测试命名空间的只读日志与 Helm 升级权限。

## 演练步骤

1. 创建临时 values，只覆盖容器启动参数为一个不存在的 Spring profile 或无效 JVM 参数。
2. 对测试 release 执行带 `--atomic --timeout 2m` 的 Helm upgrade。
3. 等待新 Pod 进入启动失败，Helm 应返回非零并自动回滚。
4. 收集以下原始证据：

```bash
kubectl -n segroup8-test get pods -o wide
kubectl -n segroup8-test get events --sort-by=.lastTimestamp
kubectl -n segroup8-test describe pod FAILED_POD
kubectl -n segroup8-test logs FAILED_POD --previous
helm -n segroup8-test history segroup8
helm -n segroup8-test status segroup8
```

5. 再次访问 liveness、readiness 和 info，确认流量仍由旧 revision 提供。

## 通过条件

- upgrade 返回非零，流水线失败。
- `helm history` 能看到失败/回滚记录。
- 原健康 revision 重新为 deployed，readiness 为 `UP`。
- 业务数据、`trade_order_request` 和 outbox 未丢失。
- 证据保存到 `04_tests/microservices/secondhand-service/evidence/logs/`，并在 `deployment-test-report.md` 填入真实 revision、SHA 和时间。

## order-service 受控降级演练

1. 在测试环境暂停 order-service 或注入超过 `ORDER_READ_TIMEOUT` 的延迟。
2. 发起一次直接购买，记录业务键与响应；本服务不得变为 NotReady。
3. 重复相同成交请求，确认不会创建第二条 `trade_order_request`。
4. 恢复 order-service，等待恢复任务；确认先查询 business key，最终只产生一个订单。
5. 将服务日志、订单查询响应和数据库唯一性结果写入 `fault-injection-report.md`。
