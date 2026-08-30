# order-service 运维交付

- [部署与回滚手册](operations-runbook.md)
- [依赖故障演练](deployment-failure-drill.md)
- [性能对比](performance-comparison.md)

Helm 模板统一位于 `deploy/helm/segroup8/templates/order-*.yaml`。生产 Secret 名默认为 `segroup8-order-secret`，不在仓库保存 Secret 内容。
