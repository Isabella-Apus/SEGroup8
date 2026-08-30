# MS-04 Issue 与 PR 记录

## 现有工作项

- Epic D：`#37`
- UC16：`#55`
- UC17：`#56`
- UC18：`#57`
- UC19：`#58`
- UC20：`#59`（由 order-service 负责履约，本服务只保留协作回归）

## 本次交付

| 项目 | 记录 |
|---|---|
| MS-04 Issue | 复用现有 EPIC-D `#37`，未重复创建 Issue；PR 使用 `Refs #37` 且不自动关闭 Epic |
| 分支 | `feature/ms-secondhand` |
| PR | `#213`：<https://github.com/Isabella-Apus/SEGroup8/pull/213> |
| 看板状态 | 已加入 `SEGroup8` Project，状态为 `In review` |
| Merge SHA | 待合并后填写 |
| 镜像 | `segroup8/secondhand:sha-<git-sha>`，digest 待 CI 推送后填写 |
| Helm revision | 待集群部署后填写 |
| 目标 tag | `microservices-v1`，由全队验收后统一创建 |

PR 描述已关联 EPIC-D `#37`，写明 21/21 Maven 结果、5/5 E2E 结果和未完成的集群证据；合并前不得提前关闭 Epic。
