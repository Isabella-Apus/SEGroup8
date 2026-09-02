# messaging-service 部署测试报告

同一 run 的 Helm lint、当前提交验收摘要、不可变镜像发布和 `Helm atomic deploy messaging-service` jobs 均成功。服务器实测镜像为 `messaging:sha-b622e6bbb0447d6823b50e7789e4777f7131eb9b`，Deployment 1/1 Ready；HTTP 探针、版本和 WebSocket 路由可用。生产 Service DNS 为 `messaging:8084`。
