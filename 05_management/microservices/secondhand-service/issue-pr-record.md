# MS-04 Issue 与 PR 记录

## 现有工作项

- Epic D：`#37`
- MS-04：`#214`（EPIC-D `#37` 的直接子 Issue）
- UC16：`#55`
- UC17：`#56`
- UC18：`#57`
- UC19：`#58`
- UC20：`#59`（由 order-service 负责履约，本服务只保留协作回归）

## 本次交付

| 项目 | 记录 |
|---|---|
| MS-04 Issue | `#214`：<https://github.com/Isabella-Apus/SEGroup8/issues/214>（父项为 EPIC-D `#37`） |
| 分支 | `feature/ms-secondhand` |
| PR | `#213`：<https://github.com/Isabella-Apus/SEGroup8/pull/213> |
| Issue/PR 关联 | PR `#213` 使用 `Closes #214`，只关闭 MS-04，不直接关闭 EPIC-D `#37` |
| 看板状态 | Issue `#214` 已加入 `SEGroup8` Project，并归属 EPIC-D `#37`；状态为 `In review` |
| CI | [Microservices CI/CD 成功](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33298506368)；[Kinda Goods CI/CD 成功](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33298506580) |
| Merge SHA | 待合并后填写 |
| 镜像 | `segroup8/secondhand:sha-<git-sha>`，digest 待 CI 推送后填写 |
| Helm revision | 待集群部署后填写 |
| 目标 tag | `microservices-v1`，由全队验收后统一创建 |

PR 描述已使用 `Closes #214` 关联唯一任务 Issue，并把 EPIC-D `#37` 标为父项。描述中写明 21/21 Maven 结果、5/5 E2E 结果和未完成的集群证据；合并 PR 只关闭 `#214`，不会提前关闭 Epic。
