# 故障注入报告

自动化已验证：余额不足时 `payment_request`、余额、流水和 Outbox 全部回滚，结果无部分提交。

自动化已验证：Outbox 下游首次不可用时事件保持未发布并记录重试时间；下游恢复后同一事件成功投递并标记为 `PUBLISHED`。

真实部署故障演练状态：`NOT_RUN`。操作步骤见 `03_devops/microservices/benefits-finance-service/deployment-failure-drill.md`，需补充停止服务、数据库断连、readiness、Pod events、脱敏日志、Helm 回滚以及相同请求 ID 不重复扣款的证据。
