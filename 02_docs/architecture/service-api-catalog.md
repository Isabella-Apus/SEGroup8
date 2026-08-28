# 服务 API 清单

## 1. 清单口径

本清单根据当前 `backend/.../controller`、`microservices/*Controller.java` 和 WebSocket 配置盘点。表中“目标服务”表示迁移后的唯一接口所有者；当前绝大多数路由仍由单体提供，不能据此宣称微服务已经部署。

统一约束：

- 外部路径继续使用 `/api/**`，由 Gateway 路由，避免前端随迁移反复改地址；
- `/internal/**` 只允许集群内部访问，必须携带服务身份、请求 ID 和幂等键；
- 登录/注册和公开查询允许匿名；其他接口使用 Bearer JWT；
- Gateway 可以注入可信身份上下文，但业务服务仍须验证签名和资源所有权；客户端提交的 `X-User-Id`、`X-Seller-Id`、`X-Admin-Id` 不可信；
- 写接口返回前必须完成本服务事务；异步副作用通过 outbox/event 处理。

## 2. 当前公开 API 的唯一目标归属

### identity-governance-service：身份模块（UC01-UC04）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| POST | `/api/auth/register` | 注册，UC01 |
| POST | `/api/auth/login` | 登录并签发 JWT，UC01 |
| GET, PUT | `/api/user/profile` | 查询/修改个人资料，UC02 |
| GET | `/api/user/me` | 当前用户摘要，UC02 |
| GET | `/api/user/search` | 用户搜索，UC02/UC24 |
| GET, POST | `/api/user/addresses` | 地址列表/新增，UC02 |
| PUT, DELETE | `/api/user/addresses/{addressId}` | 地址修改/删除，UC02 |
| POST | `/api/user/merchant-application` | 提交商家申请，UC03 |
| GET | `/api/user/merchant-application/me` | 查询本人申请，UC03 |
| GET | `/api/admin/merchant-applications` | 管理员查询申请，UC03 |
| POST | `/api/admin/merchant-applications/{applicationId}/approve` | 通过申请，UC03 |
| POST | `/api/admin/merchant-applications/{applicationId}/reject` | 驳回申请，UC03 |
| GET | `/api/admin/users` | 管理员查询用户，UC04 |
| PUT | `/api/admin/users/{userId}/ban` | 封禁账号，UC04 |
| PUT | `/api/admin/users/{userId}/unban` | 解禁账号，UC04 |

### identity-governance-service：治理模块（UC05）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| POST | `/api/report-block/report` | 提交举报，UC05 |
| GET | `/api/report-block/report/my` | 查询本人举报，UC05 |
| POST | `/api/report-block/block` | 拉黑用户，UC05 |
| DELETE | `/api/report-block/block/{targetUserId}` | 解除拉黑，UC05 |
| GET | `/api/report-block/block/my` | 拉黑列表，UC05 |
| GET | `/api/report-block/block/check/{targetUserId}` | 检查是否已拉黑，UC05 |
| GET | `/api/report-block/block/blocked-by/{targetUserId}` | 检查反向拉黑，UC05 |
| GET | `/api/credit/me`, `/api/credit/{userId}` | 本人/指定用户信用，UC05 |
| GET | `/api/admin/reports` | 管理员举报列表，UC05 |
| POST | `/api/admin/reports/audit` | 审核举报，UC05 |
| POST | `/api/admin/reports/credit-adjust` | 调整信用，UC05 |
| GET | `/api/admin/audit-logs` | 管理审计日志，UC04/UC05 |

### catalog-shop-service：catalog 模块（UC06-UC07）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| GET | `/api/category/tree` | 分类树，UC06/UC07 |
| GET | `/api/product/list` | 商品列表，UC06 |
| GET | `/api/product/detail/{productId}` | 商品详情，UC06 |
| GET | `/api/product/search` | 搜索筛选，UC06 |
| GET | `/api/product/seller/list` | 卖家商品列表，UC07 |
| GET | `/api/product/seller/{productId}` | 卖家商品详情，UC07 |
| POST | `/api/product/seller` | 新增商品，UC07 |
| PUT, DELETE | `/api/product/seller/{productId}` | 修改/删除商品，UC07 |
| POST | `/api/product/seller/{productId}/status` | 上下架，UC07 |
| POST | `/api/product/seller/{productId}/stock/adjust` | 库存调整，UC07 |

现有独立模块还暴露 `GET /api/catalog/products` 和 `GET /api/catalog/products/{id}`；迁移完成后应由 Gateway 兼容到上述公开路径。

### catalog-shop-service：risk 模块（UC09）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/admin/product-risk-audits` | 商品风险审核列表 |
| POST | `/api/admin/product-risk-audits/{auditId}/decision` | 风险审核决定 |

迁移期已有 risk 模块别名：`POST /internal/risk-audits`、`GET /api/admin/risk-audits`、`POST /api/admin/risk-audits/{id}/decision`。Gateway 最终只保留一套公开路径，旧路径在兼容期返回弃用响应头。

### catalog-shop-service：shop 模块（UC08）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/api/shop/public/{shopId}` | 公开店铺信息 |
| GET | `/api/shop/public/{shopId}/products` | 店铺商品 |
| GET | `/api/shop/seller/current` | 当前卖家店铺 |
| PUT | `/api/shop/seller/decoration` | 店铺装修 |

现有独立模块还提供 `GET /api/shops/{id}`、`GET /api/shops/seller/current`、`PUT /api/shops/seller/current/settings`、`PUT /api/shops/seller/current/decoration`。最终公开契约需要在 Gateway 统一，不能长期保留两份真相。

### catalog-shop-service：behavior 模块（UC10）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET, POST | `/api/user/browse-history` | 查询/记录浏览历史 |
| DELETE | `/api/user/browse-history/{historyId}` | 删除单条历史 |
| POST | `/api/user/browse-history/delete-batch` | 批量删除 |
| DELETE | `/api/user/browse-history/all` | 清空历史 |
| GET | `/api/search/history` | 搜索历史 |
| GET | `/api/search/hot` | 热词 |

现有独立模块提供 `POST /api/behavior/browse-history`、`GET /api/behavior/browse-history`、`DELETE /api/behavior/browse-history/{id}`、`DELETE /api/behavior/browse-history`、`POST /api/behavior/search-history`、`GET /api/behavior/search-history` 和 `GET /api/behavior/hot-keywords`。身份必须由可信 JWT 转换，不能继续信任客户端传来的用户 ID 头。

### order-service（UC11-UC15、UC20）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| POST | `/api/order/create` | 创建/拆分订单，UC11 |
| GET | `/api/order/list`, `/api/order/detail/{orderId}` | 买家订单列表/详情，UC11-UC15/UC20 |
| POST | `/api/order/{orderId}/pay`, `/api/order/{orderId}/cancel` | 支付/取消，UC12 |
| POST | `/api/order/{orderId}/ship`, `/api/order/{orderId}/remind-ship` | 发货/提醒发货，UC13/UC20 |
| POST | `/api/order/{orderId}/confirm-receive`, `/api/order/{orderId}/complete` | 收货/完成，UC13/UC20 |
| POST | `/api/order/{orderId}/refund` | 申请售后，UC14 |
| POST | `/api/order/{orderId}/refund/approve`, `/api/order/{orderId}/refund/reject` | 卖家售后决定，UC14 |
| GET | `/api/order/seller/list`, `/api/order/seller/detail/{orderId}` | 卖家订单列表/详情，UC13/UC14/UC20 |
| POST | `/api/order/{orderId}/review`, `/api/order/{orderId}/review/items` | 原始评价入口，UC15 |
| GET | `/api/review/my`, `/api/review/seller/list` | 买家/卖家评价列表，UC15 |
| POST | `/api/review/{reviewId}/reply`, `/api/review/followup` | 回复/追评，UC15 |
| POST | `/api/logistics/push-next` | 推进物流，UC13 |
| GET | `/api/logistics/order/{orderId}/trace` | 物流轨迹，UC13/UC20 |
| GET | `/api/admin/orders/list`, `/api/admin/orders/detail/{orderId}` | 管理订单，UC14 |
| POST | `/api/admin/orders/batch-close` | 批量关闭订单，UC14 |
| POST | `/api/admin/orders/{orderId}/refund/approve`, `/api/admin/orders/{orderId}/refund/reject` | 管理员仲裁，UC14 |
| GET | `/api/admin/orders/{orderId}/after-sale-logs` | 售后轨迹，UC14 |

### secondhand-service（UC16-UC19）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| GET | `/api/secondhand/list`, `/api/secondhand/detail/{productId}` | 二手列表/详情，UC16-UC19 |
| GET | `/api/secondhand/seller-public/{sellerUserId}` | 卖家公开信息，UC16 |
| GET | `/api/secondhand/seller-public/{sellerUserId}/products` | 卖家公开商品，UC16 |
| GET, POST | `/api/secondhand/seller/list`, `/api/secondhand/seller` | 本人列表/发布，UC16 |
| PUT, DELETE | `/api/secondhand/seller/{productId}` | 修改/删除，UC16 |
| POST | `/api/secondhand/seller/{productId}/status` | 上下架，UC16 |
| POST | `/api/secondhand/{productId}/buy` | 直接购买并请求创建订单，UC17 |
| POST | `/api/secondhand/trade/bargain/apply`, `/api/secondhand/trade/bargain/confirm` | 发起/确认议价，UC18 |
| POST | `/api/secondhand/trade/bargain/{negotiationId}/reject` | 拒绝议价，UC18 |
| GET | `/api/secondhand/trade/bargain/list`, `/api/secondhand/trade/bargain/effective` | 议价列表/有效议价，UC18 |
| POST | `/api/secondhand/trade/auction` | 创建拍卖，UC19 |
| GET | `/api/secondhand/trade/auction/product/{productId}` | 拍卖详情，UC19 |
| GET | `/api/secondhand/trade/auction/seller/list` | 卖家拍卖列表，UC19 |
| POST | `/api/secondhand/trade/auction/{auctionId}/close`, `/api/secondhand/trade/auction/{auctionId}/flow`, `/api/secondhand/trade/auction/{auctionId}/bid` | 提前结束、流拍、出价，UC19 |

UC20 的支付、发货、收货和完成接口归 order-service；secondhand-service 不复制订单状态机。

### benefits-finance-service（UC21-UC23）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| GET | `/api/voucher/seller/list` | 卖家券列表，UC21 |
| POST, PUT | `/api/voucher/seller`, `/api/voucher/seller/{id}` | 卖家创建/修改券，UC21 |
| POST, DELETE | `/api/voucher/seller/{id}/close`, `/api/voucher/seller/{id}` | 卖家关闭/删除券，UC21 |
| GET | `/api/voucher/admin/list` | 管理员券列表，UC21 |
| POST, PUT | `/api/voucher/admin`, `/api/voucher/admin/{id}` | 管理员创建/修改券，UC21 |
| POST, DELETE | `/api/voucher/admin/{id}/close`, `/api/voucher/admin/{id}` | 管理员关闭/删除券，UC21 |
| GET | `/api/voucher/list` | 可领取券，UC22 |
| POST | `/api/voucher/{id}/claim` | 领券，UC22 |
| GET | `/api/voucher/my`, `/api/voucher/my/available`, `/api/voucher/my/available/reasons` | 用户券与可用性，UC22 |
| GET | `/api/finance/dashboard` | 财务概览，UC23 |
| POST | `/api/finance/recharge` | 充值，UC23 |
| GET | `/api/finance/my-wallet/records` | 个人流水，UC23 |
| GET | `/api/finance/business/records` | 商家流水，UC23 |

### messaging-service（UC24-UC25）

| 方法 | 路径 | 用途/UC |
|---|---|---|
| GET, POST | `/api/chat/conversations` | 会话列表/创建，UC24 |
| GET, POST | `/api/chat/conversations/{conversationId}/messages` | 消息历史/发送，UC24 |
| GET | `/api/notifications` | 通知列表，UC25 |
| POST | `/api/notifications/{notificationId}/read` | 单条已读，UC25 |
| POST | `/api/notifications/read-all` | 全部已读，UC25 |
| WebSocket | `/ws/realtime?token=...` | 实时通知、聊天和订单事件，UC24/UC25 |

### Media adapter（支撑组件）

| 方法 | 路径 | 用途 |
|---|---|---|
| POST | `/api/upload/image` | 图片上传 |
| POST | `/api/upload/media` | 媒体上传 |

## 3. 目标内部同步契约

下列路径是迁移目标，不是当前已实现接口：

| 调用方 → 提供方 | 方法与目标路径 | 核心约束 |
|---|---|---|
| Gateway/高风险服务 → identity-governance | `POST /internal/auth/introspect` | 仅缓存缺失的高风险操作使用；禁止逐请求调用 |
| 业务服务 → identity-governance | `GET /internal/users/{userId}/summary` | 只取必要公开摘要；历史业务优先保存快照 |
| order → catalog-shop | `POST /internal/inventory/reservations` | `X-Idempotency-Key`；原子预留多个商品 |
| order → catalog-shop | `POST /internal/inventory/reservations/{id}/confirm`、`/release` | 重复调用结果一致 |
| order → benefits-finance | `POST /internal/checkout/quote` | 返回优惠明细、版本和过期时间 |
| order → benefits-finance | `POST /internal/payments/debit`、`/refund` | 资金流水与余额同事务、幂等 |
| secondhand → order | `POST /internal/orders/secondhand` | 以 `tradeType + tradeId` 唯一，重复请求返回同一订单 |
| 业务服务 → messaging | `POST /internal/notifications` | 非核心同步兼容入口；优先消费事件 |

## 4. 目标事件契约

| 事件 | 发布者 | 主要消费者 | 最小字段 |
|---|---|---|---|
| `UserAccessChanged.v1` | identity-governance | Gateway、全部受保护业务服务 | eventId、userId、status、roles、version、occurredAt |
| `MerchantApproved.v1` | identity-governance | catalog-shop、messaging | applicationId、userId、shopSnapshot、occurredAt |
| `ProductSubmitted.v1` | catalog-shop/secondhand | catalog-shop risk 模块 | productType、productId、sellerUserId、内容快照 |
| `InventoryReservationExpired.v1` | catalog-shop | order | reservationId、orderRequestId、items |
| `OrderStatusChanged.v1` | order | messaging、secondhand、benefits-finance | orderId、orderNo、oldStatus、newStatus、actorId |
| `PaymentCompleted.v1`、`RefundCompleted.v1` | benefits-finance | order、messaging | transactionId、orderId、amount、result |
| `SecondhandTradeSettled.v1` | secondhand | order、messaging | tradeType、tradeId、productId、buyerId、sellerId、price |
| `NotificationRequested.v1` | 任一业务服务 | messaging | recipientIds、type、templateData、dedupeKey |

所有事件必须版本化，消费者按 `eventId` 幂等；事件只传业务必要字段，不传密码、JWT 或完整用户隐私资料。
