# UC20 追溯矩阵

| 需求/风险 | 实现 | Integration/回归 | E2E |
| --- | --- | --- | --- |
| 只有所属卖家能发货 | `shipSellerOrder` 解析订单商品卖家并校验当前用户 | `shipmentRequiresSellerOwnershipPaymentAndPendingShipmentState` | 卖家账号从“我卖出的闲置”发货 |
| 未付款或错误状态不能发货 | `pay_status=1`、`order_status=1` 和 `version` 条件更新 | 同上 | E2E 先真实付款，再显示发货按钮 |
| 重复发货不重复物流 | 已发货订单幂等返回；只有更新赢家初始化物流 | `repeatedShipmentIsIdempotentAndCreatesOneInitialTrace` | 发货后页面显示一个首节点 |
| 买家可查看真实物流 | `LogisticsServiceImpl` 与 `/api/logistics/order/{id}/trace` | `SecondhandOrderFlowIntegrationTest` | 买家详情页显示“广东省分拨中心 / 包裹已揽收” |
| 只有买家能确认收货 | `confirmReceiveMyOrder` 校验 `buyer_user_id` | `onlyBuyerCanConfirmAndRepeatedReceiptSettlesExactlyOnce` | 买家页面执行确认收货 |
| 收货后进入待评价 | 原子更新 `order_status=3` 并写 `received_time` | 同上 | 页面显示“待评价”和“去评价” |
| 二手卖家个人钱包只结算一次 | `SecondhandSettlementStrategy` + 乐观锁余额 + 唯一订单资金流水 | 同上 | E2E 重复确认返回 400，卖家余额不再变化 |
| 结算失败整体回滚并可重试 | 收货与 `releaseEscrow` 位于同一事务 | `settlementFailureRollsBackReceiptAndRetryDoesNotDuplicateCredit` | Integration 故障注入覆盖 |
| 通知失败不回滚主交易 | `runAfterCommitBestEffort` 在提交后隔离通知/推送异常 | `notificationFailuresDoNotRollbackShipmentOrReceipt` | 真栈页面以 API/数据库状态为准 |
| 议价成交建单失败恢复商品和议价 | `confirmBargain` 事务回滚 | `bargainOrderCreationFailureRestoresNegotiationAndProduct` | Integration 故障注入覆盖 |
| 拍卖成交建单失败可重试且无重复订单 | UC19 拍卖结算事务和状态抢占 | `SecondhandAuctionLifecycleIntegrationTest.failedSettlementRollsBackAndCanRetryWithoutDuplicateOrder` | UC19 真栈与 Integration 作为前置成交保障 |
