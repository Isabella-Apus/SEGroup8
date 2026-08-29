# UC17 追溯矩阵

| 需求/风险 | 实现 | Integration | E2E |
| --- | --- | --- | --- |
| 在售商品才可购买、自购拒绝 | `buySecondhandProduct` 前置校验 | `offShelfSoldAndSelfOwnedProductsAreRejectedWithoutOrders` | 重复购买已售商品被拒绝 |
| 地址存在且属于买家 | DTO `@NotNull` + 地址归属校验 | `missingAndForeignAddressesAreRejectedBeforeProductReservation` | 真实地址选择弹窗 |
| 并发只成交一次 | `UPDATE ... WHERE status = 1` | `twoConcurrentBuyersProduceExactlyOneOrderAndOneWinner` | 刷新后按钮禁用 |
| 商品、订单、明细原子一致 | `@Transactional` | `availableProductAndOwnedAddressCreateOnePendingOrderAtomically` | 订单详情 API 与页面一致 |
| 订单写失败回滚商品 | Spring 事务回滚 | `orderInsertFailureRollsBackReservedProduct` | 不适用（故障注入由 Integration 完成） |
| 取消/支付状态正确 | `OrderServiceImpl` | `unpaidCancellationRelistsProductWhilePaymentMovesOrderToPendingShipment` | 下单后展示待付款 |
| 重复点击不重复成交 | 商品状态条件更新 | `duplicateClickCreatesOneDealAndNegotiatedPriceHonorsEffectiveWindow` | 第二次购买返回业务失败 |
| 议价价格/有效期边界 | `findEffectiveNegotiation` + 成交价校验 | 同上 | 价格由真实 API 返回 |
