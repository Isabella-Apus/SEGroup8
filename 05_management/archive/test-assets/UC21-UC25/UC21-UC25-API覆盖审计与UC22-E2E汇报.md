# UC21–UC25 API 覆盖与业务链路 E2E 报告

日期：2026-08-28
范围：成员 E（优惠券、财务、聊天、通知、WebSocket）

## 结论

- `EngagementFinanceApiAndE2ETest` 为 UC21–UC25 各提供 1 条业务链路 E2E，共 5 条。
- 每条 E2E 都经过 Spring Boot 应用上下文、鉴权、Controller、Service、Mapper 和 H2 测试库，并检查至少 1 个异常或权限分支。
- 4 个 Controller 的 26 个公开 HTTP 操作全部进入自动测试。WebSocket 测试覆盖 JWT 握手、连接确认、`PING/PONG`、通知推送和发送失败隔离。
- 定向测试共 7 条，结果为 `7 passed, 0 failed, 0 errors, 0 skipped`。

这些测试属于后端业务链路 E2E。浏览器页面自动化和截图证据需要单独运行，报告不会把 MockMvc 测试写成浏览器 E2E。

## E2E 覆盖矩阵

| 用例 | 测试方法 | 主流程 | 异常或权限分支 | 数据库证据 |
|---|---|---|---|---|
| UC21 | `uc21SellerAndAdminManageVoucherLifecycle` | 卖家和管理员创建、查询、修改、关闭、删除优惠券 | 普通用户调用管理员接口返回 403；卖家 B 修改卖家 A 的券返回 403 | 越权后折扣金额保持 10.00；关闭后 `status=0`；删除后记录数为 0 |
| UC22 | `uc22BuyerClaimsAndUsesVoucherAtCheckout` | 卖家发券，买家领券、选券、下单、支付并核销 | 50 元订单未达到 100 元门槛，接口返回“门槛不足” | `user_voucher.status=2`、`used_order_id` 匹配订单、应付 188.00、`used_count=1` |
| UC23 | `uc23RechargeWalletBusinessAccountAndRecords` | 买家充值；买家下单和支付；卖家发货；买家确认收货；系统向商家经营账户结算 | 0 元充值返回 400；买家查询经营流水返回 403；重复确认收货返回 400 | 买家余额 150.00 且只有 1 条充值流水；卖家经营余额 99.00、个人余额 0.00；重复操作后结算流水仍为 1 条 |
| UC24 | `uc24ConversationAndMessageArePersisted` | 买家创建会话并发消息；卖家从历史接口读取离线消息 | 非参与者读取和发送都返回 403 | 合法消息和通知各落库 1 条；越权发送后消息数仍为 1 |
| UC25 | `uc25NotificationReadAndRealtimePush` | 有效 token 建连；心跳响应；通知实时推送；单条和全部已读 | 非法 token 握手失败；买家不能标记卖家通知；模拟 WebSocket 发送异常 | 他人通知保持未读；发送异常后通知仍落库；全部已读后本人未读数为 0 |

## API 覆盖

| 模块 | 公开操作数 | 已覆盖 | 检查项 |
|---|---:|---:|---|
| VoucherController | 15 | 15 | 卖家和管理员 CRUD、领取、我的券、结算可用券、不可用原因、角色与所有权 |
| FinanceController | 4 | 4 | 充值、个人流水、财务看板、经营流水、账户隔离 |
| ChatController | 4 | 4 | 会话列表、创建或复用、发送消息、历史消息、参与者权限 |
| NotificationController | 3 | 3 | 本人通知、单条已读、全部已读、通知归属 |
| WebSocket | 1 条通道 | 已覆盖 | 有效和无效 JWT、连接消息、心跳、推送、失败隔离 |

## 运行方式

```powershell
cd backend
mvn -B "-Dtest=VoucherServiceTest,EngagementFinanceApiAndE2ETest" test
```

最近一次运行：

```text
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 11.094 s
Finished at: 2026-08-28T10:49:18+08:00
```

Surefire 文本报告和运行汇总保存在同目录的 `evidence/`。报告排除了含本机用户名、路径和内网地址的 XML 控制台输出。

后端全量回归：`Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`，完成时间 `2026-08-28T10:29:15+08:00`。

## PR 摘要

成员 E 已补齐 UC21–UC25 的后端业务链路 E2E。新增断言覆盖跨卖家优惠券越权、非法充值和重复结算、聊天非参与者越权、非法 WebSocket token、通知归属隔离，以及实时发送失败后的通知持久化。定向测试 7 条全部通过。
