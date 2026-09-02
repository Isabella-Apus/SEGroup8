# 03_devops 部署与实验文档

- [云原生与性能实验最终报告](cloud-native-experiments/README.md)
- `microservices/<service>/operations-runbook.md`：六个服务的部署、日志、健康/就绪、版本和回滚操作。

实际流水线位于 `.github/workflows/`，K3s/Helm 清单位于 `deploy/helm/segroup8/`，复现实验脚本位于 `scripts/experiments/cloud-native/`。本目录不再保留 PR/Issue、阶段审计和被替代实验轮次。
