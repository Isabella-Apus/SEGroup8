# 部署测试报告

状态：本次未连接生产 K3s，因此未产生 Helm revision、pod readiness 或公网 smoke 证据。已交付原子部署工作流、三个 order Helm 模板、订单域 Ingress 路由及探针配置；工作流会先检查三个下游 Service，再校验 rollout、readiness、`/actuator/info` commit 和订单查询未认证安全响应。CI 运行后仍需回填镜像 digest、revision 和原始输出。
