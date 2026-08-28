# 成员 E：UC21–UC25 页面、代码、数据与测试追溯

## 追溯矩阵

| UC | 页面 | Controller | Service | 主要数据库表 | 现有测试 | 当前判断 |
|---|---|---|---|---|---|---|
| UC21 | `SellerVoucher.vue`、`AdminVoucherView.vue` | `VoucherController` | `VoucherService` | `voucher` | `VoucherServiceTest`；`EngagementFinanceApiAndE2ETest` | 后端 E2E 覆盖卖家/管理员生命周期、普通用户越权和跨卖家越权；越权后数据库记录不变 |
| UC22 | `CouponCenterView.vue`、`ProductDetailView.vue` | `VoucherController`、`OrderController` | `VoucherService`、`OrderServiceImpl` | `voucher`、`user_voucher`、`order_info` | `VoucherServiceTest`；`EngagementFinanceApiAndE2ETest`；订单集成测试 | 后端 E2E 覆盖发券、领券、门槛判断、下单、支付、核销和金额对账 |
| UC23 | `MerchantFinanceView.vue`（以及用户钱包入口） | `FinanceController`、`OrderController` | `EscrowSettlementService`、`OrderServiceImpl` | `balance`、`transaction_record`、`order_info` | `EscrowSettlementServiceTest`；`OrderSettlementRefundFlowIntegrationTest`；`EngagementFinanceApiAndE2ETest` | 后端 E2E 覆盖充值、订单履约、经营账户结算、非法金额、经营流水权限和重复结算拦截 |
| UC24 | `ChatView.vue`（用户与商家路由共用） | `ChatController` | `ChatService` / `ChatServiceImpl` | `chat_conversation`、`chat_message` | `ChatServiceImplTest`；`EngagementFinanceApiAndE2ETest` | 后端 E2E 覆盖双用户会话、消息持久化、离线历史补取和非参与者读写拒绝 |
| UC25 | `NotificationView.vue`、`realtimeClient.js` | `NotificationController`、WebSocket 配置/处理器 | `NotificationService` / `NotificationServiceImpl` | `notification` | `RealtimeHandshakeInterceptorTest`；`EngagementFinanceApiAndE2ETest` | 后端 E2E 覆盖握手、心跳、实时推送、已读、通知归属、非法 token 和推送失败后的持久化 |

## 文件定位

页面：

- `frontend/src/views/seller/SellerVoucher.vue`
- `frontend/src/views/admin/AdminVoucherView.vue`
- `frontend/src/views/user/CouponCenterView.vue`
- `frontend/src/views/product/ProductDetailView.vue`
- `frontend/src/views/merchant/MerchantFinanceView.vue`
- `frontend/src/views/chat/ChatView.vue`
- `frontend/src/views/notification/NotificationView.vue`
- `frontend/src/realtime/realtimeClient.js`

后端：

- `backend/src/main/java/com/segroup8/platform/controller/VoucherController.java`
- `backend/src/main/java/com/segroup8/platform/controller/FinanceController.java`
- `backend/src/main/java/com/segroup8/platform/controller/ChatController.java`
- `backend/src/main/java/com/segroup8/platform/controller/NotificationController.java`
- `backend/src/main/java/com/segroup8/platform/service/VoucherService.java`
- `backend/src/main/java/com/segroup8/platform/service/ChatService.java`
- `backend/src/main/java/com/segroup8/platform/service/impl/ChatServiceImpl.java`
- `backend/src/main/java/com/segroup8/platform/service/NotificationService.java`
- `backend/src/main/java/com/segroup8/platform/service/impl/NotificationServiceImpl.java`
- `backend/src/main/java/com/segroup8/platform/service/settlement/EscrowSettlementService.java`

测试：

- `backend/src/test/java/com/segroup8/platform/service/VoucherServiceTest.java`
- `backend/src/test/java/com/segroup8/platform/service/impl/ChatServiceImplTest.java`
- `backend/src/test/java/com/segroup8/platform/service/settlement/EscrowSettlementServiceTest.java`
- `backend/src/test/java/com/segroup8/platform/realtime/RealtimeHandshakeInterceptorTest.java`
- `backend/src/test/java/com/segroup8/platform/integration/OrderSettlementRefundFlowIntegrationTest.java`
- `backend/src/test/java/com/segroup8/platform/integration/EngagementFinanceApiAndE2ETest.java`

## 测试编号建议（供成员 C 的全组规则冻结前使用）

临时采用 `层级-UCxx-三位序号`，如 `UNIT-UC21-001`、`INT-UC23-002`、`E2E-UC25-001`。成员 C 发布全组最终规则后只做机械替换，REQ/UC 追溯关系不变。

## 验证 SQL（只读）

```sql
SELECT id, seller_user_id, status, total_count, used_count FROM voucher ORDER BY id DESC;
SELECT user_id, voucher_id, status, use_time FROM user_voucher ORDER BY id DESC;
SELECT user_id, balance, business_balance FROM balance ORDER BY user_id;
SELECT user_id, amount, trade_type, related_order_id FROM transaction_record ORDER BY id DESC;
SELECT id, user1_id, user2_id, last_message_time FROM chat_conversation ORDER BY id DESC;
SELECT conversation_id, sender_user_id, receiver_user_id, create_time FROM chat_message ORDER BY id DESC;
SELECT id, user_id, is_read, create_time FROM notification ORDER BY id DESC;
```

说明：字段名以运行 Schema 为准；验证脚本不得执行更新或删除，不输出消息正文、JWT、密码或密钥。浏览器页面自动化和截图证据仍需在前后端服务启动后单独采集。
