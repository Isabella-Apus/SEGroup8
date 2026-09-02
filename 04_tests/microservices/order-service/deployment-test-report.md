# order-service 部署测试报告

同一 run 的 Helm lint、不可变镜像发布和 `Helm atomic deploy order-service` jobs 均成功。服务器实测镜像为 `order:sha-b622e6bbb0447d6823b50e7789e4777f7131eb9b`，Deployment 1/1 Ready；liveness、readiness、`/actuator/info` 和公开健康检查返回 200。Order 依赖故障正式结果见 `04_tests/cloud-native-experiments/20260902-order-fault-b622e6bb/`。
