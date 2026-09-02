# benefits-finance-service 部署测试报告

同一 run 的 Helm lint、当前提交验收摘要、精确候选镜像发布和 `Atomic Helm deploy benefits-finance-service` jobs 均成功。服务器实测镜像为 `benefits-finance:sha-b622e6bbb0447d6823b50e7789e4777f7131eb9b`，Deployment 1/1 Ready；liveness、readiness、`/actuator/info` 和公开健康检查返回 200。余额与流水的只读对账命令保留在 operations runbook。
