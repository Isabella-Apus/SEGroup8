# MS-04 交付清单

- [x] 独立 Spring Boot/Maven 模块、Flyway、自有 MySQL schema
- [x] 服务划分图、接口清单、数据表归属、跨服务调用说明
- [x] monolith 基线与微服务版本差异说明
- [x] 21 个公开业务操作及 2 个内部事件操作的自动化测试与运行时 OpenAPI 完整性门禁
- [x] UC16-UC19 独立服务镜像/MySQL API E2E
- [x] UC16-UC20 完整系统浏览器 E2E
- [x] JWT、内部服务令牌、所有权、幂等、CAS、恢复与 outbox 测试
- [x] 非 root 运行时镜像，且镜像从已测试 JAR 制作
- [x] 独立服务流水线、不可变 SHA 标签与 digest artifact
- [x] 保存 JAR SHA-256、候选 Image ID 和 release metadata；独立 E2E 后原样 tag/push，不重新构建
- [x] Helm Deployment/Service、startup/liveness/readiness、资源限制
- [x] JSON 日志、请求关联、健康/就绪/存活/版本查询
- [x] 共享生产部署锁、原子升级、失败诊断和自动回滚
- [x] 完整 Playwright HTML/trace/video 改为 Actions artifact，Git 只留紧凑证据
- [x] HPA `autoscaling/v2`、资源请求/限制、CI 静态门禁、1→3→1 预实验与 1→4→1 正式本地实验
- [x] 同机、同数据、同脚本的二手拍卖单体/微服务三轮正式性能对比及 CSV/JSON/raw 证据
- [x] 真实 HTTP 订单依赖停机、恢复、重试耗尽和幂等恢复的本地隔离演练
- [x] 本地隔离 Kubernetes 错误镜像部署、事件/describe/history 诊断与 Helm 自动回滚
- [ ] 本次提交对应的远程 Actions 结果（推送后记录，不预先声称通过）
- [ ] main 生产镜像 digest、Helm revision 与真实 K8s rollout（仅生产 run 可生成）
- [ ] 全队真实 order-service 停止/恢复演示（本服务已完成进程级真实 HTTP 契约依赖演练）
- [ ] 商品搜索、新品下单的单体/微服务三轮对比（依赖 B/C 服务及统一数据环境）
- [ ] 非作者 Review 与部署失败演示截图（由评审/部署环境完成）

基线 tag 为 `monolith-start`，目标 tag 为 `microservices-v1`。
