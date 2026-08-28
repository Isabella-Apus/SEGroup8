# UC18 追溯矩阵

| 需求/风险 | 实现 | Integration | E2E |
| --- | --- | --- | --- |
| 发起议价真实写库 | `applyBargain` 写 `APPLIED` | `applicationPersistsAndBothParticipantsCanListIt` | 买家从商品详情提交 ¥76 |
| 商品议价资格、自购和价格边界 | 商品/身份/金额前置校验 | `invalidNonNegotiableSelfAndRepeatedApplicationsAreRejected` | 使用真实可议价商品 |
| 重复有效议价拒绝 | 查询 `APPLIED`/有效 `CONFIRMED` | 同上 | 单次申请后状态为待处理 |
| 非商品卖家无权处理 | sellerUserId 所有权校验 | `unrelatedSellerCannotConfirmOrReject` | 卖家账号查看本人商品会话 |
| 确认/拒绝并发只有一个结果 | `UPDATE ... WHERE status = APPLIED` | `concurrentConfirmAndRejectProduceExactlyOneDecision` | 卖家按钮处理一次后不可重复操作 |
| 确认后建单一致性 | 同一事务更新议价、商品、订单和明细 | `confirmationCreatesOnePendingPaymentOrderAtConfirmedPrice` | 买家刷新看到“去支付订单” |
| 拒绝结束本次议价 | 原子更新为 `REJECTED` | `rejectionEndsApplicationWithoutCreatingOrder` | 确认分支之外由 Integration 覆盖 |
| 聊天/通知失败不回滚核心决定 | `afterCommit` + `runBestEffort` | `chatAndNotificationFailuresDoNotRollbackCoreDecision` | 真实聊天卡片双端同步 |
| 卖家聊天页可查询议价 | 新增参与者隔离的 `/bargain/list` | 双方列表断言 | 卖家聊天页显示申请并确认 |
