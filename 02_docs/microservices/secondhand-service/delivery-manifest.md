# MS-04 交付清单

- [x] 独立 Spring Boot/Maven 模块、Flyway、自有 MySQL schema
- [x] 服务划分图、接口清单、数据表归属、跨服务调用说明
- [x] monolith 基线与微服务版本差异说明
- [x] 21/21 公开 API 的自动化测试及运行时 OpenAPI 完整性门禁
- [x] UC16-UC19 独立服务镜像/MySQL API E2E
- [x] UC16-UC20 完整系统浏览器 E2E
- [x] JWT、内部服务令牌、所有权、幂等、CAS、恢复与 outbox 测试
- [x] 非 root 运行时镜像，且镜像从已测试 JAR 制作
- [x] 独立服务流水线、不可变 SHA 标签与 digest artifact
- [x] Helm Deployment/Service、startup/liveness/readiness、资源限制
- [x] JSON 日志、请求关联、健康/就绪/存活/版本查询
- [x] 共享生产部署锁、原子升级、失败诊断和自动回滚
- [x] 完整 Playwright HTML/trace/video 改为 Actions artifact，Git 只留紧凑证据
- [x] HPA `autoscaling/v2`、资源请求/限制、CI 静态门禁与 1→3→1 本地预实验
- [ ] 本次提交对应的远程 Actions 结果（推送后记录，不预先声称通过）
- [ ] main 生产镜像 digest、Helm revision 与真实 K8s rollout（仅生产 run 可生成）
- [ ] 真实 order-service 停止/恢复、错误镜像参数和 Helm 回滚集群演示
- [ ] 非作者 Review 与部署失败演示截图（由评审/部署环境完成）

基线 tag 为 `monolith-start`，目标 tag 为 `microservices-v1`。
