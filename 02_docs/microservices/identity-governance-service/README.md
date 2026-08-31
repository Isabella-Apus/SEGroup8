# identity-governance-service 交付索引

本目录是 MS-01 的唯一架构交付入口。服务独占 UC01-UC05 的身份与治理事实，公开路径继续使用 `/api/**`，内部协作使用 `/internal/**`。实现位于 `microservices/identity-governance-service/`，默认端口 `8091`，数据库为 `identity_governance_db`。

## 本次范围

- 已实现：独立 Spring Boot JAR、Flyway、MySQL/H2 测试、Dockerfile、本地 Compose、JWT、UC01-UC05 公开 API、内部 API、outbox、健康/就绪/版本信息和 JSON 日志。
- 已验证：Maven 独立测试、真实 MySQL 全链路、31 个公开 method-path 的成功/鉴权/失效账户状态矩阵、UC01-UC05 在改造前后两版上的同断言 E2E。
- 已接入：独立 `Identity Governance Service CI/CD` 与完整系统集成门禁并行运行；服务流水线负责 ACR 不可变镜像和 K3s/Helm `--atomic --wait` 独立升级，完整系统流水线负责跨域回归。远端 CI/CD 要以对应 GitHub run 为准。
- 暂不实验：HPA 自动扩缩容实验、停止/延迟依赖服务的故障处理实验；HPA 模板默认关闭，两个实验均标为 `NOT_RUN`。性能对比在全组微服务版本稳定后统一执行。

## 文件导航

- `service-boundary.md`：职责与迁移边界
- `service-diagram.mmd` / `service-diagram.svg`：可编辑源图与可查看图
- `openapi.yaml`：公开和内部 API 契约
- `service-api-list.md`：31 个公开接口与 3 个内部接口的人工核对清单
- `database-ownership.md`：表归属与权限验证
- `cross-service-calls.md`：事件、幂等和失败处理
- `migration-version-report.md`：单体基线与迁移状态
- `before-after-code-diff.md`：改造前后版本、同断言结果和代码结构差异
- `traceability.md`：UC 到代码和测试的追溯
- `delivery-manifest.md`：交付状态与复现命令
- `../three-service-course-comparison.md`：身份治理、订单、二手服务按课程要求的逐项差距与阶段分类
