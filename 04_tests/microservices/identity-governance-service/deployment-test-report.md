# identity-governance-service 部署测试报告

同一 run 的不可变镜像发布与 `Helm atomic deploy identity-governance-service` jobs 均成功。服务器实测镜像为 `identity-governance:sha-b622e6bbb0447d6823b50e7789e4777f7131eb9b`，Deployment 1/1 Ready；liveness、readiness、`/actuator/info` 和公开健康检查均返回 200。部署脚本使用 `helm upgrade --atomic --cleanup-on-fail --wait`，失败后不继续 smoke。
