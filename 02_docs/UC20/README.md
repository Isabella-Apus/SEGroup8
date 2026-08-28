# UC20 二手成交后的订单履约

## 范围

UC20 承接直接购买、议价成交或拍卖成交后已经生成的二手订单，完成“买家付款、卖家发货、买家查看物流、买家确认收货、担保资金结算、进入待评价”的履约闭环。

## 核心规则

- 只有二手商品所属卖家可以发货；订单必须已付款且处于待发货状态。
- 发货使用订单版本号和状态条件原子更新；重复发货直接返回当前结果，不重复创建物流轨迹。
- 买家可在真实订单详情页查看物流节点，只有订单买家可以确认收货。
- 确认收货后订单进入 `RECEIVED(3)`，页面显示“待评价”，不会直接跳成已完成。
- 二手订单通过 `SecondhandSettlementStrategy` 将担保资金结算到卖家个人钱包，并写入唯一资金流水。
- 重复确认收货按既有契约返回 `400`，且不重复结算；结算失败时订单状态和资金变动整体回滚，可安全重试。
- 通知和实时推送在事务提交后以 best-effort 执行，失败不会回滚已经成功的发货或收货。
- 议价或拍卖成交建单失败时，原事务回滚并保留可重试状态，避免商品被错误售出或生成残缺订单。

## 实现与验证入口

- 服务：`OrderServiceImpl`、`EscrowSettlementService`、`SecondhandSettlementStrategy`
- API：`OrderController`、`LogisticsController`
- Integration：`SecondhandFulfillmentLifecycleIntegrationTest`
- 兼容回归：`SecondhandOrderFlowIntegrationTest`、`OrderControllerUc20WebMvcTest`
- E2E：`frontend/e2e/domain-d/uc20-fulfillment.spec.ts`
- 测试证据：`04_tests/UC20/evidence/`

父用例：#59；本阶段 Task：#152；Epic：#37。
