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
| CI | 上一版：[Microservices CI/CD 成功](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33298506368)；[Kinda Goods CI/CD 成功](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33298506580)。当前提交推送后重新核对 |
| Merge SHA | 待合并后填写 |
| 镜像 | `segroup8/secondhand:sha-<git-sha>`，digest 待 CI 推送后填写 |
| 本地 Helm 回滚 | 隔离集群 revision 3 failed，revision 4 rollback to 2；生产 revision 待 main 部署 |
| 目标 tag | `microservices-v1`，由全队验收后统一创建 |

PR 描述使用 `Closes #214` 关联唯一任务 Issue，并把 EPIC-D `#37` 标为父项。当前本地证据为 Maven 26/26、
独立服务 E2E 4/4、Domain D E2E 5/5、正式 HPA、二手拍卖六轮性能数据、订单故障恢复和 Helm 回滚。
合并 PR 只关闭 `#214`，不会提前关闭 Epic；生产部署和非作者 Review 仍单独保留。
