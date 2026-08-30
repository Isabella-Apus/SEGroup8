# MS-01 交付清单

| 项目 | 状态 | 证据/命令 |
|---|---|---|
| 独立 Maven Boot JAR | PASS | `mvn ... clean verify` |
| Flyway + 独立 Schema | PASS | H2 和 MySQL 8.4 Testcontainers |
| UC01-UC05 服务 API | PASS | 公开/内部控制器与自动测试 |
| JWT/角色/所有权/非法状态 | PASS | API、集成和契约测试 |
| Dockerfile / 本地 Compose | PASS | 镜像构建成功；服务与 MySQL 容器均 healthy |
| JSON 日志、liveness/readiness/info | PASS | 三项运行检查为 UP/正确版本，日志为 JSON |
| 数据库跨 Schema 拒绝 | PASS | 本 Schema COUNT 成功；跨查返回 MySQL 1142 |
| Domain A 真实浏览器 E2E | PASS | 既有 UC01-UC05 spec 5/5，经临时 Nginx → 独立服务 → MySQL |
| 改造前后同断言版本 | PASS | `monolith-start` 5/5；微服务 5/5，Git 保留 JSON/XML，完整报告由 Actions artifact 保存 |
| 全公开接口 API 测试 | PASS | 31/31 成功；29/29 匿名与失效账户；10/10 管理越权 |
| GitHub Actions PR 门禁 | PASS | 独立服务 run `33297661588` 与完整系统 run `33297661706` 均成功 |
| ACR/K3s 发布部署 | CONFIGURED / NOT_RUN | 仅合并到 `main` 后发布；PR run 中 publish/deploy 按设计跳过 |
| Helm Deployment/Service/HPA 模板 | CONFIGURED | Deployment/Service 可由服务流水线启用；HPA 默认关闭，启用形态纳入静态渲染门禁 |
| HPA 与依赖故障两个云原生实验 | NOT_RUN | 用户明确暂不执行这两个实验 |
| PR、非作者 Review、merge/tag | PARTIAL / NOT_RUN | 本轮创建中文 PR；非作者 Review、merge/tag 仍需其他组员 |

不把 `IMPLEMENTED` 或 `CONFIGURED` 等同于运行通过；最终状态以 `04_tests/.../result-summary.json` 为准。
