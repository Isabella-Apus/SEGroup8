# UC21–UC25 API 覆盖审计与 UC22 E2E 汇报

日期：2026-08-27
范围：成员 E（优惠券、财务、聊天、通知、WebSocket）

## 结论

- 4 个 Controller 共 26 个公开 HTTP 操作，现已全部进入 `EngagementFinanceApiAndE2ETest`，HTTP 操作覆盖率为 `26/26`。
- WebSocket 同时覆盖有效 token 握手、连接确认、`PING/PONG` 和通知实时推送。
- 补测过程中发现普通用户可调用优惠券管理员接口，已在 `VoucherService` 增加官方卖家/管理员角色校验，并增加 `code=403` 自动断言。
- 汇报用例选择 **UC22 领券及结算使用**。它跨越优惠券、订单、支付和数据库状态，业务闭环清晰，比单纯列表或已读操作更适合现场说明。

## API 覆盖矩阵

| 模块 | 公开操作数 | 已覆盖 | 主要验证 |
|---|---:|---:|---|
| VoucherController | 15 | 15 | 卖家/管理员 CRUD、可领取列表、领取、我的券、结算可用券与不可用原因、角色越权 |
| FinanceController | 4 | 4 | 充值、个人流水、财务看板、经营流水与账户隔离 |
| ChatController | 4 | 4 | 会话列表、创建/复用、发消息、历史消息与落库 |
| NotificationController | 3 | 3 | 本人通知列表、单条已读、全部已读 |
| WebSocket | 1 条通道 | 已覆盖 | JWT 握手、连接消息、心跳、实时通知推送 |

## UC22 自动化 E2E 闭环

测试方法：`uc22BuyerClaimsAndUsesVoucherAtCheckout`

1. 官方卖家通过 API 创建满 100 减 10 的店铺券。
2. 买家查询领券中心并领取该券。
3. 买家查询“我的优惠券”，断言状态为未使用。
4. 以 50 元结算上下文查询不可用原因，断言返回“门槛不足”。
5. 以店铺 100、金额 198 查询可用券，断言该券可选。
6. 买家通过 `/api/order/create` 购买 2 件 99 元商品并携带券。
7. 断言订单原价 198、优惠 10、应付 188。
8. 买家通过 `/api/order/{id}/pay` 完成支付。
9. 数据库断言 `user_voucher.status=2`、`used_order_id=订单ID`、`order_info.payable_amount=188.00`、`voucher.used_count=1`。

这条测试经过 Controller、鉴权拦截器、订单服务、优惠券服务、Mapper 和 H2 测试数据库，属于后端业务链路 E2E；浏览器级双端 UI 自动化可作为后续增强，不与本报告中的后端 E2E 混称。

## 汇报建议（约 90 秒）

“我们选择 UC22，因为它不是单接口演示，而是从卖家发券、买家领券一直走到订单支付。测试先验证低于门槛时系统会说明不可用原因；满足门槛后创建 198 元订单，优惠 10 元，应付 188 元。支付完成后，再从数据库核对用户券状态、绑定订单、订单应付金额和优惠券使用次数，证明页面金额和后端状态是一致的。补测试时还发现普通用户原本能访问管理员券接口，我们增加了角色校验和 403 回归断言。”

## 运行方式

```powershell
cd backend
mvn -B "-Dtest=VoucherServiceTest,EngagementFinanceApiAndE2ETest" test
```

本次定向结果：`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`。
后端全量回归：`Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。
