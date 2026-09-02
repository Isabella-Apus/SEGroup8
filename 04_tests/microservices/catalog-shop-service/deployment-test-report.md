# catalog-shop-service 部署测试报告

同一 run 的 `Publish immutable tested image` 与 `Atomic K3s deployment and smoke` jobs 成功。服务器实测镜像为 `catalog-shop:sha-b622e6bbb0447d6823b50e7789e4777f7131eb9b`，Deployment 1/1 Ready；探针、版本和公开健康检查返回 200。Helm 使用实际 Service DNS `segroup8-catalog-shop:8080`，部署失败由原子升级回滚。
