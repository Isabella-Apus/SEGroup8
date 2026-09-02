# secondhand-service 部署测试报告

同一 run 的 Helm lint、不可变镜像发布和 `Helm atomic deploy secondhand-service` jobs 均成功。服务器实测镜像为 `secondhand:sha-b622e6bbb0447d6823b50e7789e4777f7131eb9b`，Deployment 1/1 Ready；探针、版本和健康检查返回 200。二手专属 HPA 已移除，完整系统 HPA 和 Order 自动恢复分别见两份 20260902 正式实验报告。
