# MS-04 交付清单

- [x] `microservices/secondhand-service` 独立 Boot 模块、Dockerfile、Flyway
- [x] 服务划分图、二手状态/成交图及 SVG 交付
- [x] UC16-UC19 兼容公网 API
- [x] JWT 鉴权、所有权和非法状态保护
- [x] 商品/议价/拍卖 CAS 并发控制
- [x] 表归属、跨库写拒绝及无跨领域 Mapper 证明
- [x] `tradeType + tradeId` 订单契约、business key 查询和重试上限
- [x] outbox 与事件消费幂等
- [x] `monolith-start` 与微服务版本的 API/事件改造差异说明
- [x] liveness/readiness/info/version
- [x] unit/api/integration/contract 测试源码
- [x] 公开商品/直购/议价/拍卖及订单事件 Controller 级 API 测试
- [x] Docker 29 兼容的 Testcontainers MySQL 权限测试
- [x] Docker 镜像与 Helm 模板
- [x] 独立 CI/CD 工作流
- [x] UC16-UC20 Playwright spec 单一引用，不复制测试
- [x] 本地 Maven 21/21、Domain D E2E 5/5、Docker 与 Helm 验证报告
- [ ] PR Actions 成功链接、镜像 digest、Helm revision（推送 PR 后填写）
- [ ] 非作者 Review 与部署失败演示截图（由评审/部署环境完成）

基线：`monolith-start`；目标 tag：`microservices-v1`。负责人 `Chazeynnn`，评审人由组长分配且不得与负责人相同。
