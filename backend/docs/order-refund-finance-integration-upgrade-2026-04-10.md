# 订单履约与财务隔离升级说明（2026-04-10）

## 1. 目标与范围
本次升级聚焦你提出的三项需求：

1. 卖家 7 天不处理退货，系统自动退款。
2. `tradeType` 统一为固定字典枚举，并在前端统一中文标签展示。
3. 提供一套可执行的集成测试，覆盖：送达 -> 自动确认 -> 结算 -> 财务流水隔离 -> 退款分流。

同时补充了实现所需的接口返回字段和前端展示逻辑，确保业务闭环可观测、可验证。

---

## 2. 功能新增与行为变化

### 2.1 新增：超时自动退款定时任务（卖家未处理 7 天）
- 调度类：`OrderAutoConfirmScheduler`
- 新任务：`autoApproveTimeoutRefundOrders()`
- 调度表达式：`0 15 * * * ?`（每小时第 15 分钟执行）
- 命中条件：
  - `refund_status = PROCESSING(1)`
  - `refund_apply_time <= 当前时间 - 7 天`
  - `refund_mode IS NULL OR refund_mode = RETURN_REFUND`
- 处理动作：
  - 自动将退款状态置为 `APPROVED`
  - 订单状态置为 `CLOSED`
  - 决策来源置为 `SYSTEM`
  - 买家账户执行退款回流（个人钱包）
  - 记录售后日志与实时事件

实现入口：
- `OrderService.autoApproveRefundForSystem(Long orderId)`
- `OrderServiceImpl.autoApproveRefundForSystem(Long orderId)`

### 2.2 枚举化：交易流水 tradeType 固定字典
新增枚举：`TransactionTradeTypeEnum`

固定字典值：
- `INCOME_PERSONAL` -> 个人账户入账
- `INCOME_BUSINESS` -> 经营账户入账
- `EXPENSE_PURCHASE` -> 消费支出
- `RECHARGE` -> 钱包充值
- `REFUND_BACKFLOW` -> 退款回流
- `UNKNOWN` -> 未知类型

后端统一改造点：
- `EscrowSettlementService` 中所有交易流水写入改为枚举 code
- 退款、支付、充值等调用点不再写自由字符串，统一传枚举

### 2.3 财务接口增强：返回 tradeType 中文名
`FinanceRecordVO` 新增字段：`tradeTypeName`

返回逻辑：
- 原始值仍保留 `tradeType`
- 新增 `tradeTypeName = TransactionTradeTypeEnum.of(tradeType).desc`

### 2.4 前端统一展示交易类型中文标签
新增工具：`frontend/src/utils/finance.js`
- 优先显示后端返回的 `tradeTypeName`
- 如果后端无该字段，按本地字典兜底映射 `tradeType`

应用页面：
- 商家财务页 `MerchantFinanceView.vue`
- 个人资料钱包流水 `Profile.vue`

---

## 3. 业务流程（使用说明）

### 3.1 退货退款超时自动处理流程
1. 买家发起 `RETURN_REFUND`（退货退款）。
2. 订单进入 `退款中`。
3. 卖家 7 天未处理。
4. 定时任务命中后自动退款：
   - 订单关闭
   - 退款状态置为已退款
   - 决策来源标记为 SYSTEM
   - 买家个人钱包回流
   - 产生流水记录（`REFUND_BACKFLOW`）

### 3.2 财务流水显示流程
1. 后端写入 `tradeType` 枚举 code。
2. 财务接口返回 `tradeType + tradeTypeName`。
3. 前端列表统一显示中文名称（后端字段优先，本地兜底）。

---

## 4. 前后端接口变更

### 4.1 后端 Service 接口
文件：`backend/src/main/java/com/segroup8/platform/service/OrderService.java`

新增方法：
- `void autoApproveRefundForSystem(Long orderId)`

### 4.2 财务接口返回结构
文件：`backend/src/main/java/com/segroup8/platform/vo/FinanceRecordVO.java`

新增字段：
- `tradeTypeName: String`

接口：
- `GET /api/finance/my-wallet/records`
- `GET /api/finance/business/records`

返回中新增：
- `tradeTypeName`

---

## 5. 数据库变更

### 5.1 生产库结构
本轮功能不强制新增线上表字段（基于既有字段实现调度与流水枚举化）。

### 5.2 测试库结构（集成测试专用）
新增测试 SQL：
- `backend/src/test/resources/integration/full-flow-setup.sql`

内容包括：
- 创建/补齐测试表：`shop`、`product`、`secondhand_product`、`balance`、`transaction_record`
- 对 `order_info` 补齐履约与退款相关列（若不存在）
- 注入三类订单测试数据（自动确认、仅退款、超时退款）

---

## 6. 集成测试设计与覆盖

### 6.1 新增测试类
- `backend/src/test/java/com/segroup8/platform/integration/OrderSettlementRefundFlowIntegrationTest.java`

### 6.2 覆盖场景
#### 场景 A：送达 -> 自动确认 -> 结算 -> 财务隔离
- 调用：`orderService.autoConfirmReceiveForSystem(301)`
- 断言：
  - 订单状态从 `SHIPPED` -> `RECEIVED`
  - 售后截止时间生成
  - 卖家经营账户余额增加
  - 卖家个人账户不变
  - 流水 `tradeType = INCOME_BUSINESS`

#### 场景 B：退款分流（仅退款 + 超时自动退款）
- `ONLY_REFUND`：买家主动发起后立即回流
- `RETURN_REFUND`：卖家超时 7 天后由调度自动回流
- 断言：
  - 两类订单最终均进入已退款/已关闭
  - 超时单 `refundDecisionSource = SYSTEM`
  - 买家个人账户累计回流金额正确
  - 两条退款流水 `tradeType = REFUND_BACKFLOW`

### 6.3 执行方式
在 `backend` 目录执行：

```bash
mvn -q -DskipTests compile
mvn -q -Dtest=OrderSettlementRefundFlowIntegrationTest test
```

---

## 7. 关键代码清单

后端：
- `backend/src/main/java/com/segroup8/platform/common/TransactionTradeTypeEnum.java`
- `backend/src/main/java/com/segroup8/platform/common/RefundDecisionSourceEnum.java`
- `backend/src/main/java/com/segroup8/platform/service/OrderService.java`
- `backend/src/main/java/com/segroup8/platform/service/impl/OrderServiceImpl.java`
- `backend/src/main/java/com/segroup8/platform/schedule/OrderAutoConfirmScheduler.java`
- `backend/src/main/java/com/segroup8/platform/service/settlement/EscrowSettlementService.java`
- `backend/src/main/java/com/segroup8/platform/controller/FinanceController.java`
- `backend/src/main/java/com/segroup8/platform/vo/FinanceRecordVO.java`

前端：
- `frontend/src/utils/finance.js`
- `frontend/src/views/merchant/MerchantFinanceView.vue`
- `frontend/src/views/user/Profile.vue`

测试：
- `backend/src/test/java/com/segroup8/platform/integration/OrderSettlementRefundFlowIntegrationTest.java`
- `backend/src/test/resources/integration/full-flow-setup.sql`

---

## 8. 验证结果
已完成本地验证：

1. 后端编译通过
- `mvn -q -DskipTests compile` -> `EXIT_CODE=0`

2. 新增集成测试通过
- `OrderSettlementRefundFlowIntegrationTest` -> 通过

3. 关键单元测试通过
- `OrderServiceImplTest` -> 通过

4. 前端构建通过
- `npm run build` -> 成功

---

## 9. 后续建议
1. 若线上已有历史自由格式 `trade_type` 数据，建议做一次离线清洗映射到标准字典。
2. 为自动退款任务补充告警埋点：处理条数、失败条数、失败订单 ID。
3. 可增加管理后台“系统自动退款”筛选维度（按 `refund_decision_source=SYSTEM`）。
