# 微服务最终验收固定检查清单

这是仓库内的验收基线副本，适用于每个业务微服务。每项必须记录实际文件、命令、结果和证据；未执行统一标为 `NOT_RUN`，不得由配置或预期替代运行结果。

| 项目 | 最低要求 |
|---|---|
| 元数据 | 服务、负责人、分支、PR、基线/当前 commit、日期和结论可追溯 |
| 文档 | 边界、图、OpenAPI、表归属、跨服务调用、前后差异、追踪表齐全 |
| 独立性 | 独立 Maven 模块、配置、Flyway、Dockerfile、真实 MySQL 和最小下游 stub |
| 写接口 | `Idempotency-Key` 同键同请求重放；同键不同请求明确拒绝 |
| 可观测性 | JSON 日志、requestId/traceId、liveness/readiness/info 和受控 Actuator 端点 |
| 测试 | Maven、全部公开 API、真实 MySQL、独立候选镜像 E2E、归属 UC 浏览器 E2E 分层记录 |
| 供应链 | 已验证 JAR 的 SHA、不可变候选镜像、非 root UID/GID、精确发布镜像 |
| CI/CD | 独立命名工作流，完整 E2E 和 Helm 静态门禁；共享 release 使用统一串行锁 |
| K8s | DNS 与真实 Service 一致、三类探针、资源/安全上下文、原子升级与 rollout smoke |
| 证据 | 原始 JSON/XML、关键日志、失败证据、当前 commit 和实际状态；避免提交完整临时报告 |

最终验收只能在本地验证、独立服务 E2E、真实浏览器路由 E2E、候选镜像、main 发布 digest 和目标 K8s rollout 均有当前提交证据后标记 `PASS`。本清单的逐项 MS-05 状态见 `benefits-finance-service/delivery-manifest.md`。
