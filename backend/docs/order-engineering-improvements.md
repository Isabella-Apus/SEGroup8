# 订单管理工程化改进说明

## 本轮改进目标

- 消除订单售后核心流程中的魔法值，提升可维护性与可读性。
- 提升重复请求防护能力，降低重复点击/网络重试导致的重复执行风险。
- 提升可观测性，保证每次请求都有可追踪 `traceId`。
- 强化数据库查询性能基础，为订单筛选场景增加复合索引。

## 已完成内容

### 1) 强类型状态与动作

- 新增枚举：
  - `RefundStatusEnum`
  - `RefundDecisionSourceEnum`
  - `AfterSaleActionEnum`
  - `OperatorRoleEnum`
- `OrderStateMachine` 改为基于 `RefundStatusEnum` 校验售后状态流转。
- `OrderServiceImpl` 和 `AdminOrderController` 将退款状态、审核来源、售后日志动作和操作角色改为枚举驱动。

### 2) 幂等保护（接口层）

- 新增 `IdempotencyInterceptor`，支持请求头：`X-Idempotency-Key`。
- 对 `/api/order/**` 与 `/api/admin/orders/**` 的非 GET 请求启用重复提交防护。
- 同一用户 + 同一路径 + 同一幂等键在短时间内重复请求会返回 `409`。

### 3) 可观测性（Trace）

- 新增 `TraceIdInterceptor`：
  - 自动读取/生成 `X-Trace-Id`。
  - 回写响应头 `X-Trace-Id`。
  - 记录结构化请求日志：`traceId / method / path / status`。

### 4) 售后日志去重

- `OrderServiceImpl` 与 `AdminOrderController` 的售后日志写入增加“最近同动作同操作者同备注去重”判断，减少重复日志污染。

### 5) 索引优化

- `order_info` 新增索引：
  - `idx_order_info_status_refund_create(order_status, refund_status, create_time)`
  - `idx_order_info_refund_create(refund_status, create_time)`

## 后续建议

- 生产环境引入持久化幂等表（当前为内存短期防护）。
- 增加“并发冲突与重复提交”的集成测试用例。
- 将关键业务日志接入统一日志平台，以 `traceId` 聚合查询。

