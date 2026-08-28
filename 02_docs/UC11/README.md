# UC11 购物车结算与创建订单设计

## 设计范围

本文对应 `REQ11 / UC11`，覆盖新品购物车或立即购买创建待付款订单。支付、取消和支付后流程归 `UC12`。

## 设计视图

- [系统交互图](system.mmd)
- [组件图](component.mmd)
- [对象图](object.mmd)
- [追溯矩阵](traceability.md)

## 组件职责与归属

| 层级 | 组件 | 职责 | 归属 |
|---|---|---|---|
| 前端 | `CartView.vue`、`ProductDetailView.vue` | 采集商品、数量、地址和优惠券，提交结算并展示订单详情 | 订单结算前端 |
| API | `OrderController#create` | 校验 `CreateOrderRequest`，返回 `Result<OrderVO>` | 订单域 |
| 横切 | `JwtAuthInterceptor`、`IdempotencyInterceptor`、`TraceIdInterceptor` | 身份、重复提交保护和链路标识 | 平台基础设施 |
| 领域服务 | `OrderServiceImpl#createOrder` | 合并商品项、读取服务端价格、校验交易权限、扣库存并创建订单 | 订单域 |
| 协作服务 | `VoucherService#occupyForOrder` | 校验并占用优惠券，计算折扣承担额与应付金额 | 优惠券域 |
| 数据访问 | `ProductMapper`、`ShopMapper`、`AddressMapper`、`OrderInfoMapper`、`OrderItemMapper`、`UserBlockMapper` | 访问商品、店铺、地址、订单和拉黑关系 | 各实体所属域 |

## 数据表归属

| 表 | 所属组件 | UC11 职责 |
|---|---|---|
| `order_info`、`order_item` | 订单域 | 保存待付款订单聚合与明细 |
| `product`、`shop` | 商品/店铺域 | 提供可信价格、库存和卖家归属；扣减库存 |
| `address` | 用户域 | 校验地址归属并保存收货快照 |
| `voucher`、`user_voucher` | 优惠券域 | 校验、占用优惠券并保存折扣分摊 |
| `user_block` | 访问控制 | 拒绝存在拉黑关系的交易 |
| `idempotency_record` | 平台基础设施 | 保存幂等执行状态和首次响应 |

## API 与事务约束

`POST /api/order/create` 接收 `items[]`、可选 `addressId`、`voucherId` 和 `remark`。成功订单必须为 `PENDING_PAY(0)`、`payStatus=0`，金额和明细仅由服务端商品数据计算。

`createOrder` 使用 `@Transactional(rollbackFor = Exception.class)`。库存、订单主从记录和优惠券占用共同提交或回滚；地址必须属于当前买家；买家不得购买本人店铺商品；买卖双方任一方向存在拉黑关系时拒绝交易；相同 `X-Idempotency-Key` 在有效期内回放首次结果。

## 验证入口

- Integration：`OrderCreateUc11IntegrationTest`
- Browser E2E：`frontend/e2e/domain-c/uc11-checkout-order.spec.ts`
- 测试计划：[04_tests/UC11/test-plan.md](../../04_tests/UC11/test-plan.md)
- 测试报告：[04_tests/UC11/test-report.md](../../04_tests/UC11/test-report.md)

