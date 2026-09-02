# 微服务架构交付索引

本目录是最终微服务架构事实来源。A–E 是需求与团队交付域，运行时为六个业务微服务；两者不要混用。

| 文档 | 内容 | 当前状态 |
|---|---|---|
| [微服务划分与依赖设计](microservice-boundaries.md) | 服务划分图、职责、同步/异步依赖和故障影响 | 六服务已实现并部署 |
| [服务 API 清单](service-api-catalog.md) | 所有公开接口所有者及内部 API/事件 | 已按当前控制器与路由更新 |
| [数据库表归属方案](database-ownership.md) | 单体表到六个独立 schema 的唯一写入者 | 六服务 Flyway 已建表；单体保留兼容回退 |

六个服务都具备独立 Maven 模块、Dockerfile、MySQL/Flyway、API/真实数据库/E2E 测试、独立流水线、不可变镜像、Helm Deployment/Service、探针、版本与日志。共享 `security-contract` 是库，不是第七个业务服务。

证据边界：源码或配置存在只能证明“已实现”；测试与部署结论必须指向相同提交的 Actions、镜像和 Kubernetes 运行记录。历史证据保留其原始提交号，不冒充当前提交结果。
