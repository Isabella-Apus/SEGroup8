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
| Domain A 真实浏览器 E2E | NOT_RUN | 复用既有 5 个 spec；尚未把根网关切到本服务 |
| GitHub Actions | CONFIGURED / NOT_RUN | 分支未 push，无 Actions run |
| Kubernetes/Helm/HPA/云原生实验 | OUT_OF_SCOPE | 用户明确暂不做云原生 |
| PR、非作者 Review、merge/tag | NOT_RUN | 需要仓库权限和其他组员参与 |

不把 `IMPLEMENTED` 或 `CONFIGURED` 等同于运行通过；最终状态以 `04_tests/.../result-summary.json` 为准。
