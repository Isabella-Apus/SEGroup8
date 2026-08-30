# 故障注入报告

自动契约测试已验证：finance debit 与结果查询均返回 UNKNOWN 时，pay 返回 503 `PAYMENT_TEMPORARILY_UNAVAILABLE`，订单保持可查询的 `PAYMENT_PENDING`。真实 Kubernetes 缩容/延迟注入尚未执行，按运维演练文档回填日志与恢复证据。
