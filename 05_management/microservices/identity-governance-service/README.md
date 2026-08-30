# MS-01 管理证据

| 字段 | 当前值 |
|---|---|
| Task | MS-01 identity-governance-service 迁移与独立交付 |
| 分支 | `feature/ms-identity-governance` |
| 负责人 | 待团队填写 |
| 非作者评审人 | 待团队填写，不能与负责人相同 |
| 验收条件 | 独立构建/测试、表归属、API 契约、UC01-UC05 回归和可复现材料 |
| Issue / PR / Review | PR 将由本轮创建；Issue 与非作者 Review 未完成时使用 `Refs`，不得写 `Closes` |
| Kubernetes 部署 | 已配置独立服务 CI/CD；完整系统 CI 作为并行集成门禁，远端执行以 Actions run 为准 |
| 云原生实验 | HPA 自动扩缩容、依赖故障处理均 `NOT_RUN`；不是整个 Kubernetes 部署出范围 |

合并前应由非作者复核 `delivery-manifest.md` 和原始报告；在 E2E、容器权限测试、PR Review 未完成前，Issue 只能写 `Refs`，不能写 `Closes`。
