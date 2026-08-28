# Kinda Goods 软件概要设计说明书

> 文档基线：2026-08-28，代码基线 `09db0eed`
> 覆盖范围：`UC01`–`UC25`
> 编号规则：组件结构图使用 `COMP-STRUCTxx`，组件顺序图使用 `COMP-SEQxx`。
> 状态口径：`CURRENT` 表示仓库中可运行实现，`TARGET` 表示后续微服务边界。目标设计不得作为已部署证据。

## 1 架构与职责

前端使用 Vue 3，原系统后端使用 Spring Boot、MyBatis-Plus 和 MySQL。当前 `microservices/` 已实现 Domain B 的 `catalog-service`、`shop-service`、`risk-service` 和 `behavior-service` 四个独立原型模块；其他业务仍由单体承载。目标架构收敛为 6 个可独立构建、测试和部署的业务服务。API 网关、数据库和前端不计入业务微服务数量。

```mermaid
flowchart LR
  Browser[Vue 3 前端] --> Gateway[API 网关/反向代理]
  Gateway --> Identity[identity-governance-service]
  Gateway --> Catalog[catalog-shop-service]
  Gateway --> Order[order-service]
  Gateway --> Secondhand[secondhand-service]
  Gateway --> Benefits[benefits-finance-service]
  Gateway --> Messaging[messaging-service]
  Identity --> IdentityDB[(identity_governance_db)]
  Catalog --> CatalogDB[(catalog_shop_db)]
  Order --> OrderDB[(order_db)]
  Secondhand --> SecondhandDB[(secondhand_db)]
  Benefits --> BenefitsDB[(benefits_finance_db)]
  Messaging --> MessagingDB[(messaging_db)]
  Order --> Catalog
  Order --> Benefits
  Secondhand --> Order
  Order --> Messaging
  Identity --> Messaging
```

## 2 组件边界

| 组件 | 主要职责 | 核心数据 |
|---|---|---|
| `identity-governance-service` | UC01–UC05：身份、资料、商家申请、封禁、举报拉黑和信用治理 | 身份治理相关表 |
| `catalog-shop-service` | UC06–UC10：类目商品、店铺、风险审核和行为统计 | 目录、商品、店铺、风险与行为表 |
| `order-service` | UC11–UC15、UC20：新品订单、支付、物流、售后、评价和履约 | 订单、明细、售后、物流与评价表 |
| `secondhand-service` | UC16–UC19：二手发布、直购入口、议价与拍卖 | 二手商品、议价与拍卖表 |
| `benefits-finance-service` | UC21–UC23：优惠券、钱包和资金结算 | 券、余额与资金流水表 |
| `messaging-service` | UC24–UC25：会话、消息、通知与实时推送 | 会话、消息与通知表 |

服务只能写自己管理的表。跨服务读取使用 API，跨服务写操作使用 API 或事件；调用失败时由发起方回滚、重试或记录待补偿任务。详细边界、接口和表归属分别见 `../architecture/microservice-boundaries.md`、`../architecture/service-api-catalog.md` 和 `../architecture/database-ownership.md`。当前六个目标服务尚未全部实现，不能据此声称微服务阶段完成。

## 3 分用例组件顺序模型

每个用例另有与本节顺序图配套的组件结构图，统一位于 `../UCxx/component.mmd`；顺序图的独立 Mermaid 源位于 `../UCxx/component-sequence.mmd`。结构图表达职责和依赖，顺序图表达一次场景交互，两者不互相替代。

## UC01 注册、登录和角色鉴权

### COMP-SEQ01

```mermaid
sequenceDiagram
  autonumber
  participant UI as Login/Register.vue
  participant API as AuthController
  participant Svc as AuthServiceImpl
  participant UserDB as UserMapper
  participant JWT as JwtUtils/JwtAuthInterceptor
  UI->>API: 注册或登录请求
  API->>Svc: register/login(request)
  Svc->>UserDB: 查询或保存用户
  Svc->>JWT: 签发 JWT
  JWT-->>UI: Token 和角色
  UI->>JWT: 携带 Token 访问接口
  JWT-->>API: 注入当前用户
```

## UC02 用户资料和地址

### COMP-SEQ02

```mermaid
sequenceDiagram
  autonumber
  participant UI as Profile/AddressManager.vue
  participant API as UserController
  participant Svc as UserServiceImpl
  participant UserDB as UserMapper
  participant AddrDB as AddressMapper
  UI->>API: 查询或保存资料/地址
  API->>Svc: 当前用户操作
  Svc->>UserDB: 读写本人资料
  Svc->>AddrDB: 校验归属并维护默认地址
  AddrDB-->>UI: 最新资料与地址
```

## UC03 商家申请、通过和拒绝

### COMP-SEQ03

```mermaid
sequenceDiagram
  autonumber
  participant UserUI as MerchantApplyView.vue
  participant AdminUI as AdminMerchantReviewView.vue
  participant API as AdminMerchantApplicationController
  participant Svc as MerchantApplicationServiceImpl
  participant DB as merchant_application/user/shop
  participant Audit as AdminAuditLogService
  UserUI->>Svc: 提交申请
  Svc->>DB: 保存 PENDING
  AdminUI->>API: 通过或拒绝
  API->>Svc: review(applicationId, decision)
  Svc->>DB: 更新申请、角色和店铺
  Svc->>Audit: 写审核记录
```

## UC04 用户封禁、解禁及审计

### COMP-SEQ04

```mermaid
sequenceDiagram
  autonumber
  participant UI as AdminUserList.vue
  participant API as AdminUserController
  participant Svc as AdminUserServiceImpl
  participant DB as UserMapper
  participant Audit as AdminAuditLogServiceImpl
  UI->>API: 封禁/解禁用户
  API->>Svc: changeStatus(adminId, userId)
  Svc->>DB: 更新用户状态
  Svc->>Audit: 记录操作者、目标和原因
  Audit-->>UI: 返回结果和审计信息
```

## UC05 举报、拉黑、信用分和审计

### COMP-SEQ05

```mermaid
sequenceDiagram
  autonumber
  participant UI as Credit/AdminReport.vue
  participant API as ReportBlockController/AdminReportController
  participant Svc as ReportBlockServiceImpl
  participant Credit as CreditServiceImpl
  participant DB as user_report/user_block/credit_score_log
  UI->>API: 举报、拉黑或审核
  API->>Svc: 校验身份和目标
  Svc->>DB: 保存关系或审核状态
  opt 举报成立
    Svc->>Credit: adjustScore(targetUserId)
    Credit->>DB: 写信用分日志
  end
  DB-->>UI: 返回状态
```

## UC06 商品列表、搜索筛选和详情

### COMP-SEQ06

```mermaid
sequenceDiagram
  autonumber
  participant UI as 商品列表/详情页
  participant API as CatalogController
  participant Svc as CatalogService
  participant DB as products
  UI->>API: GET /api/catalog/products
  API->>Svc: search(filters)
  Svc->>DB: 查询 ON_SALE 商品
  DB-->>Svc: 商品集合
  Svc-->>UI: 过滤并排序的结果
  UI->>API: GET /api/catalog/products/{id}
  API->>Svc: publicDetail(id)
  Svc->>DB: 查询在售详情
  DB-->>UI: 商品详情/不存在
```

## UC07 卖家商品新增、编辑、上下架和库存调整

### COMP-SEQ07

```mermaid
sequenceDiagram
  autonumber
  participant SellerProductEdit
  participant ProductController
  participant ProductService
  participant ProductMapper
  participant Outbox
  participant RiskService
  participant SellerProductList
  SellerProductEdit->>ProductController: 提交保存/审核/上下架/库存
  ProductController->>ProductService: DTO 与身份
  ProductService->>ProductMapper: 条件更新商品
  ProductMapper->>Outbox: 记录状态变更事件
  Outbox->>RiskService: 待审核事件
  RiskService->>ProductService: 审核决定回调
  ProductService->>SellerProductList: 返回最新状态
```

## UC08 店铺查看、设置和装修

### COMP-SEQ08

```mermaid
sequenceDiagram
  autonumber
  participant PublicUI as PublicShopView.vue
  participant SellerUI as ShopSetting/Decoration.vue
  participant API as ShopController
  participant Svc as ShopServiceImpl
  participant DB as ShopMapper
  PublicUI->>API: GET 公开店铺
  API->>Svc: getPublicShop(shopId)
  Svc->>DB: 查询 OPEN 店铺
  SellerUI->>API: PUT 设置/装修
  API->>Svc: 校验店主和配置
  Svc->>DB: 更新店铺
  DB-->>SellerUI: 最新配置
```

## UC09 商品风险审核

### COMP-SEQ09

```mermaid
sequenceDiagram
  autonumber
  participant UI as AdminProductRiskAuditView.vue
  participant API as AdminProductRiskAuditController
  participant Svc as ProductRiskAuditServiceImpl
  participant Rule as 风险规则/LLM适配器
  participant DB as ProductRiskAuditMapper
  participant Catalog as 商品服务回调
  UI->>API: 查询待审/提交决定
  API->>Svc: audit(productId, decision)
  Svc->>Rule: 计算或读取风险结果
  Svc->>DB: 保存审核和理由
  Svc->>Catalog: 回写商品状态
  Catalog-->>UI: 返回审核结果
```

## UC10 浏览记录、搜索历史和热词

### COMP-SEQ10

```mermaid
sequenceDiagram
  autonumber
  participant UI as Product/BrowseHistory.vue
  participant API as SearchController/BrowseHistoryController
  participant Svc as SearchBehaviorService/BrowseHistoryService
  participant DB as history/keyword tables
  UI->>API: 浏览商品或搜索
  API->>Svc: record(userId, event)
  Svc->>DB: 去重写入并累计关键词
  UI->>API: 查询/删除历史和热词
  API->>Svc: list/delete/currentUser
  Svc->>DB: 按用户与时间过滤
  DB-->>UI: 历史与热词
```

## UC11 购物车结算和订单拆分

### COMP-SEQ11

```mermaid
sequenceDiagram
  autonumber
  participant UI as CartView.vue
  participant API as OrderController
  participant Order as OrderServiceImpl
  participant Catalog as 商品目录接口
  participant DB as order_info/order_item
  UI->>API: POST /api/order/create(items,addressId)
  API->>Order: createMyOrders(request)
  Order->>Catalog: 批量校验价格、状态和库存
  Catalog-->>Order: 商品快照/失败
  Order->>Order: 按新品店铺和二手单件拆分
  Order->>DB: 事务写入订单和明细
  DB-->>UI: 待付款订单列表
```

## UC12 支付和取消

### COMP-SEQ12

```mermaid
sequenceDiagram
  autonumber
  participant UI as OrderView.vue
  participant API as OrderController
  participant Order as OrderServiceImpl
  participant Finance as EscrowSettlementService
  participant Idem as IdempotencyInterceptor
  participant DB as order/balance/transaction
  UI->>Idem: 支付或取消请求 + 幂等键
  Idem->>API: 首次请求
  API->>Order: payMyOrder/cancelMyOrder
  Order->>Finance: 扣款或释放预留
  Finance->>DB: 更新余额并写流水
  Order->>DB: 更新状态/恢复库存
  DB-->>UI: 稳定结果或幂等回放
```

## UC13 发货、物流、收货和完成

### COMP-SEQ13

```mermaid
sequenceDiagram
  autonumber
  participant UI as MerchantOrders/OrderDetail.vue
  participant API as OrderController
  participant Order as OrderServiceImpl
  participant Logistics as LogisticsServiceImpl
  participant Scheduler as OrderAutoConfirmScheduler
  participant Finance as EscrowSettlementService
  UI->>API: 发货/确认收货
  API->>Order: ship/confirmReceive
  Order->>Logistics: 初始化或读取轨迹
  opt 到期未确认
    Scheduler->>Order: autoConfirm(orderId)
  end
  Order->>Finance: 释放担保资金
  Finance-->>UI: 待评价订单和结算结果
```

## UC14 退款、退货及仲裁

### COMP-SEQ14

```mermaid
sequenceDiagram
  autonumber
  participant BuyerUI as AfterSaleView.vue
  participant AdminUI as AdminOrderView.vue
  participant API as OrderController/AdminOrderController
  participant Order as OrderServiceImpl
  participant Finance as EscrowSettlementService
  participant DB as order_info/order_after_sale_log
  BuyerUI->>API: 提交退款/退货申请
  API->>Order: applyRefund(request)
  Order->>DB: 保存 PROCESSING 和日志
  AdminUI->>API: 审核或仲裁
  API->>Order: approve/reject
  opt 审核通过
    Order->>Finance: refund(order, amount)
  end
  Order->>DB: 保存最终状态和处理人
```

## UC15 评价、追评和卖家回复

### COMP-SEQ15

```mermaid
sequenceDiagram
  autonumber
  participant BuyerUI as MyReviews/OrderDetail.vue
  participant SellerUI as MerchantReviews.vue
  participant OrderAPI as OrderController
  participant OrderSvc as OrderServiceImpl
  participant ReviewAPI as ReviewController
  participant DB as ReviewMapper/order_item
  BuyerUI->>OrderAPI: 提交首评
  OrderAPI->>OrderSvc: 校验订单、用户和重复首评
  OrderSvc->>DB: 写 ORIGINAL 评价
  BuyerUI->>ReviewAPI: 提交追评
  ReviewAPI->>DB: 校验原评价并写 FOLLOW_UP
  SellerUI->>ReviewAPI: 回复评价
  ReviewAPI->>DB: 校验卖家归属并写回复
  DB-->>BuyerUI: 最新评价
```

## UC16 二手商品发布和管理

### COMP-SEQ16

```mermaid
sequenceDiagram
  autonumber
  participant UI as 发布闲置/二手管理页
  participant API as SecondhandProductController
  participant Svc as SecondhandProductService
  participant Mapper as SecondhandProductMapper
  participant DB as secondhand_product
  UI->>API: POST /api/secondhand/seller
  API->>Svc: createSellerProduct(userId, request)
  Svc->>Mapper: insert(SecondhandProduct)
  Mapper->>DB: 写入商品
  UI->>API: GET /api/secondhand/seller/list
  API->>Svc: pageSellerProducts(query)
  Svc-->>UI: PageVO<SecondhandProductVO>
```

## UC17 二手直接购买及禁止自购

### COMP-SEQ17

```mermaid
sequenceDiagram
  autonumber
  participant UI as 二手详情/订单页
  participant API as SecondhandProductController
  participant Svc as SecondhandProductService
  participant Order as OrderService
  participant DB as order_info/order_item
  UI->>API: POST /api/secondhand/{productId}/buy
  API->>Svc: buySecondhandProduct(buyerId, request)
  Svc->>Order: createSecondhandOrder(...)
  Order->>DB: 写入待付款订单和明细
  UI->>Order: POST /api/order/{orderId}/pay
  Order->>DB: 更新为 PENDING_SHIP
```

## UC18 议价申请、确认和拒绝

### COMP-SEQ18

```mermaid
sequenceDiagram
  autonumber
  participant UI as 二手详情/聊天/我的闲置
  participant API as SecondhandTradeController
  participant Svc as SecondhandTradeService
  participant Neg as ProductNegotiationMapper
  participant Order as OrderService
  UI->>API: POST /api/secondhand/trade/bargain/apply
  API->>Svc: applyBargain(buyerId, request)
  Svc->>Neg: insert(PENDING)
  UI->>API: POST /api/secondhand/trade/bargain/confirm
  API->>Svc: confirmBargain(sellerId, request)
  Svc->>Order: create order with bargain price
```

## UC19 拍卖、出价和结束

### COMP-SEQ19

```mermaid
sequenceDiagram
  autonumber
  participant UI as 二手详情/二手管理
  participant API as SecondhandTradeController
  participant Svc as SecondhandTradeService
  participant Auction as ProductAuctionMapper
  participant Log as AuctionLogMapper
  UI->>API: POST /api/secondhand/trade/auction
  API->>Svc: createAuction(sellerId, request)
  Svc->>Auction: insert(ONGOING)
  UI->>API: POST /api/secondhand/trade/auction/{auctionId}/bid
  API->>Svc: placeBid(buyerId, request)
  Svc->>Auction: update currentPrice/leader
  Svc->>Log: insert bid log
```

## UC20 二手成交后的订单履约

### COMP-SEQ20

```mermaid
sequenceDiagram
  autonumber
  participant UI as 二手卖出/订单页
  participant OrderAPI as OrderController
  participant OrderSvc as OrderService
  participant Logistics as LogisticsService
  participant DB as order_info/logistics_trace
  UI->>OrderAPI: GET /api/order/seller/list?productType=SECONDHAND
  OrderAPI->>OrderSvc: pageSellerOrders(query)
  UI->>OrderAPI: POST /api/order/{orderId}/ship
  OrderAPI->>OrderSvc: shipSellerOrder(orderId, request)
  OrderSvc->>Logistics: create first trace
  OrderSvc->>DB: orderStatus=SHIPPED
  UI->>OrderAPI: POST /api/order/{orderId}/confirm-receive
  OrderSvc->>DB: orderStatus=RECEIVED
```

## UC21 卖家/管理员优惠券生命周期

### COMP-SEQ21

```mermaid
sequenceDiagram
  autonumber
  participant UI as SellerVoucher/AdminVoucher.vue
  participant API as VoucherController
  participant Svc as VoucherService
  participant Shop as ShopMapper
  participant DB as VoucherMapper
  UI->>API: 创建/编辑/关闭/删除
  API->>Svc: 传入当前用户和请求
  Svc->>Shop: 解析卖家真实店铺
  Svc->>DB: 校验所有权并写入 voucher
  DB-->>UI: 最新优惠券状态
```

## UC22 领券及结算使用

### COMP-SEQ22

```mermaid
sequenceDiagram
  autonumber
  participant UI as CouponCenter/ProductDetail.vue
  participant API as VoucherController
  participant Voucher as VoucherService
  participant Order as OrderServiceImpl
  participant DB as voucher/user_voucher/order_info
  UI->>API: 领取优惠券
  API->>Voucher: claim(userId, voucherId)
  Voucher->>DB: 写 user_voucher
  UI->>Order: 提交订单(voucherId)
  Order->>Voucher: occupyForOrder(orderContext)
  Voucher->>DB: 校验并核销
  Order->>DB: 保存优惠金额和应付金额
```

## UC23 充值、个人钱包、商家账户和结算

### COMP-SEQ23

```mermaid
sequenceDiagram
  autonumber
  participant UI as Wallet/MerchantFinance.vue
  participant API as FinanceController
  participant Svc as EscrowSettlementService
  participant Balance as BalanceMapper
  participant Tx as TransactionRecordMapper
  participant Order as OrderServiceImpl
  UI->>API: 充值或查询账户
  API->>Svc: recharge/dashboard/records
  Svc->>Balance: 乐观锁更新个人余额
  Svc->>Tx: 写充值流水
  Order->>Svc: 订单完成后结算
  Svc->>Balance: 更新个人或经营余额
  Svc->>Tx: 写关联订单流水
```

## UC24 会话和消息

### COMP-SEQ24

```mermaid
sequenceDiagram
  autonumber
  participant UI as ChatView.vue
  participant API as ChatController
  participant Svc as ChatServiceImpl
  participant Conv as ChatConversationMapper
  participant Msg as ChatMessageMapper
  participant RT as RealtimePushService
  UI->>API: 创建/查询会话
  API->>Svc: 校验参与者和来源
  Svc->>Conv: 查询或插入会话
  UI->>API: 发送消息
  API->>Svc: sendMessage(currentUser, request)
  Svc->>Msg: 持久化消息
  Svc->>RT: 推送接收者
```

## UC25 通知、已读和实时推送

### COMP-SEQ25

```mermaid
sequenceDiagram
  autonumber
  participant Biz as 订单/议价/审核模块
  participant Svc as NotificationServiceImpl
  participant DB as NotificationMapper
  participant RT as RealtimePushService
  participant WS as RealtimeWebSocketHandler
  participant UI as NotificationView.vue
  Biz->>Svc: create(userId, event)
  Svc->>DB: INSERT notification
  Svc->>RT: pushToUser(userId)
  RT->>WS: 发送实时事件
  UI->>Svc: 查询/单条已读/全部已读
  Svc->>DB: 按当前用户更新
  DB-->>UI: 最新通知列表
```

## 4 跨组件失败处理

| 场景 | 处理方式 |
|---|---|
| 订单读取商品失败 | 终止下单并返回稳定错误码，不写订单 |
| 优惠券核销后订单保存失败 | 同一事务回滚；跨服务模式记录补偿任务并释放用户券 |
| 通知或 WebSocket 推送失败 | 核心业务继续提交，通知先落库并记录推送失败 |
| 风险审核回调商品服务失败 | 保留审核记录和回调状态，重试后按版本号更新商品 |
| 拍卖结算创建订单失败 | 保留待结算状态和冻结资金，定时任务按幂等键重试 |
| 服务超时或不可用 | 客户端收到明确的降级结果；调用方记录 TraceId 和依赖服务名 |
