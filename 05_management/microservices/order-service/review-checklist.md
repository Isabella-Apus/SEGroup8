# 非作者评审清单

- [ ] 评审人与作者不同，身份和时间可追溯
- [ ] order-service 无禁止 Mapper 或跨库 SQL
- [ ] 状态机、金额和乐观锁规则正确
- [ ] 支付/退款没有无条件 HTTP 自动重试
- [ ] 幂等键重复请求返回同一资源，换 payload 被拒绝
- [ ] outbox 与 Saga 失败状态可见且可恢复
- [ ] JWT、内部 token、订单所有权和管理员权限有测试
- [ ] MySQL `order_app` 跨库查询真实被拒绝
- [ ] Helm 探针、Secret/ConfigMap、资源和 sha 镜像正确
- [ ] API/E2E/故障/性能原始报告与汇总一致
