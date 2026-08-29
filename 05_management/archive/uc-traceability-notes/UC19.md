# UC19 追溯矩阵

| 需求/风险 | 实现 | Integration | E2E |
| --- | --- | --- | --- |
| 卖家发起拍卖且可重新拍卖历史商品 | `createAuction` 校验所有权、在售状态和 `ONGOING` 记录 | `sellerCanCreateAfterHistoricalAuctionButDuplicateAndNonOwnerAreRejected` | 卖家从真实详情页发起拍卖 |
| 首次价、加价幅度和拍卖时间约束 | `placeBid` 按是否已有最高出价计算最低合法价 | `nonexistentFutureClosedExpiredAndSelfBidsAreRejected` | 两名买家分别出价 ¥50、¥60 |
| 卖家禁止自购 | sellerUserId 与当前用户比较 | 同上 | 使用独立买家账号竞拍 |
| 出价冻结和被超价退款 | `EscrowSettlementService.changePersonalBalance` 与竞价同事务 | `legalBidsPersistLogsAndReleaseThePreviousBidderFunds` | API 重查两名买家余额 |
| 每次合法出价可审计 | 写入 `auction_log(ACCEPTED)` | 同上 | 卖家工作台显示 2 次出价和最高出价人 |
| 并发出价只有一个最高价 | `version` 条件更新，冲突事务回滚 | `concurrentBidsLeaveExactlyOneLeaderAndOneFundHold` | 顺序真栈竞价，Integration 覆盖并发 |
| 只有拍卖卖家可提前结束 | `getOwnedOngoingAuction` | `onlySellerCanCloseAndNoBidAuctionFlowsWithoutAnOrder` | 卖家工作台操作本人拍卖 |
| 无出价流拍不建单 | `SETTLING/FLOW` 分支，下架商品 | 同上 | 由 Integration 覆盖 |
| 成交只创建一个订单 | 原子 `SETTLING -> FINISHED`，写订单、明细和 `settledOrderId` | `sellerCloseCreatesOnePaidPendingShipmentOrderAndItem` | 赢家订单页显示已付款待发货 |
| 结算失败可重试且不重复 | `@Transactional` 整体回滚，状态条件更新 | `failedSettlementRollsBackAndCanRetryWithoutDuplicateOrder` | Integration 失败注入覆盖 |
