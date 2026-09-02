# order-service 运维交付

- [部署与回滚手册](operations-runbook.md)

Helm 模板统一位于 `deploy/helm/segroup8/templates/order-*.yaml`。生产 Secret 名默认为 `segroup8-order-secret`，不在仓库保存 Secret 内容。

流水线发布的是通过独立 UC11-UC15/UC20 API E2E 的同一候选镜像，不在发布 job 重新编译。每次运行可下载 JAR、候选镜像及校验元数据、Playwright/Compose 证据、镜像 digest 和 Kubernetes 部署诊断。

课程依赖故障选择“二手 → Order”作为典型链路，HPA、故障恢复和改造前后性能的正式报告统一位于 `03_devops/cloud-native-experiments/README.md`。
