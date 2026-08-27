# UC05 测试报告

后端集成测试覆盖：举报/查询、单向拉黑与双向可见性、取消拉黑幂等错误、管理员审核扣分、`credit_score_log` 与 `admin_audit_log` 一致性、重复审核、自举报、自拉黑和非管理员拒绝。

当前 H2、MockMvc 结果写入 `evidence/result-summary.json`。Docker Compose Playwright 需要在 Docker 服务可用时运行；未运行前不标记为通过。
