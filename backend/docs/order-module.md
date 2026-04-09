## 订单模块实现说明（功能深化版 B）

### 模块目标

本模块面向三类角色提供订单全流程能力：

- 买家：我的订单列表、详情、支付、取消、确认收货、去评价（完成订单）。
- 卖家：卖家订单分页查询、发货。
- 管理员：订单分页查询、订单详情、批量关闭订单。

订单状态页签与业务状态严格对应：

- `待付款(0)`、`待发货(1)`、`待收货(2)`、`待评价(3)`、`已完成(4)`、`已关闭(9)`。

同时提供“更像淘宝”的售后能力：

- 买家可提交退货/退款申请（原因 + 凭证图）。
- 卖家/管理员可对退货申请作出同意/拒绝决策，并记录审核意见与审核人。
- 前端在订单详情、售后列表、后台订单管理中展示“订单进度 + 售后进度时间线”，并展示审核结论。

### 后端接口清单

#### 买家侧（需登录）

- `GET /api/order/list`：分页查询我的订单（支持 `orderStatus/refundStatus` + `keyword` + `startTime/endTime/minAmount/maxAmount`）。
- `GET /api/order/detail/{orderId}`：获取我的订单详情。
- `POST /api/order/{orderId}/pay`：支付订单（`待付款 -> 待发货`）。
- `POST /api/order/{orderId}/cancel`：取消订单（未关闭/未完成订单可关闭）。
- `POST /api/order/{orderId}/confirm-receive`：确认收货（`待收货 -> 待评价`）。
- `POST /api/order/{orderId}/complete`：完成订单（`待评价 -> 已完成`，前端按钮文案“去评价”）。
- `POST /api/order/{orderId}/refund`：提交退货/退款申请（写入 `refundStatus=1`、`refundApplyTime`、`refundReason`、`refundProofUrls`）。

#### 卖家侧（需登录）

- `GET /api/order/seller/list`：分页查询卖家相关订单（支持状态和关键词）。
- `POST /api/order/{orderId}/ship`：卖家发货（`待发货 -> 待收货`，含归属校验）。
- `POST /api/order/{orderId}/refund/approve`：卖家同意退货（写入 `refundStatus=2`，并写入 `refundDecisionTime/refundDecisionUserId/refundDecisionRemark`）。
- `POST /api/order/{orderId}/refund/reject`：卖家拒绝退货（写入 `refundStatus=3`，并写入 `refundDecisionTime/refundDecisionUserId/refundDecisionRemark`）。

#### 管理员侧（需管理员登录）

- `GET /api/admin/orders/list`：分页查询全部订单（支持 `orderStatus/refundStatus` + `keyword` + `startTime/endTime/minAmount/maxAmount`）。
- `GET /api/admin/orders/detail/{orderId}`：查询订单详情。
- `POST /api/admin/orders/batch-close`：批量关闭订单（跳过不存在/已完成/已关闭订单）。
- `POST /api/admin/orders/{orderId}/refund/approve`：管理员同意退货（支持 request body：`{ "remark": "审核意见(可选)" }`）。
- `POST /api/admin/orders/{orderId}/refund/reject`：管理员拒绝退货（支持 request body：`{ "remark": "审核意见(可选)" }`）。
- `GET /api/admin/orders/{orderId}/after-sale-logs`：查询该订单的售后操作记录（用于后台审计展示）。

### 数据结构与字段说明（关键字段）

#### 订单表 `order_info`（节选）

- **售后状态**
  - `refund_status`：0无售后、1退款中、2已退款、3退款被拒绝
  - `refund_reason`：买家申请理由（支持“原因+补充说明”拼接）
  - `refund_proof_urls`：退款凭证图片 URL，逗号分隔
- **售后时间与审核信息**
  - `refund_apply_time`：买家申请时间
  - `refund_decision_time`：卖家/管理员处理时间
  - `refund_decision_user_id`：审核人（卖家或管理员）的用户 ID
  - `refund_decision_remark`：审核意见（管理员可输入；卖家默认“卖家同意/拒绝退货”）
  - `refund_decision_source`：审核来源（`SELLER`/`ADMIN`），用于前端展示“卖家/平台”
- **订单时间线字段**
  - `paid_time/shipped_time/received_time/completed_time/closed_time`：分别对应支付/发货/收货/完成/关闭时间

#### 返回对象 `OrderVO`（节选）

前端用于“订单进度/售后进度时间线”展示的核心字段：

- `refundStatus/refundStatusName/refundReason/refundProofUrls`
- `refundApplyTime/refundDecisionTime`
- `refundDecisionUserId/refundDecisionUserName/refundDecisionRemark`
- `refundDecisionSource`（`SELLER`/`ADMIN`）
- `paidTime/shippedTime/receivedTime/completedTime/closedTime`

#### 售后操作记录表 `order_after_sale_log`

用于审计“谁在什么时间做了什么售后动作”，更像淘宝后台。

- `order_id`：关联订单
- `action`：`APPLY`(买家申请)、`APPROVE`(同意)、`REJECT`(拒绝)
- `operator_user_id/operator_role`：操作者与角色（BUYER/SELLER/ADMIN）
- `remark`：原因/意见（用于前端/后台展示）
- `create_time`：记录时间

### 状态流转说明（概要）

#### 订单主流程

- `待付款(0)` -> 支付 -> `待发货(1)`
- `待发货(1)` -> 发货 -> `待收货(2)`
- `待收货(2)` -> 确认收货 -> `待评价(3)`
- `待评价(3)` -> 完成/评价 -> `已完成(4)`
- 任意未完成订单可取消 -> `已关闭(9)`（写入 `closed_time`）

#### 售后流程（退款/退货）

- 买家提交申请：`refund_status=1`，写入 `refund_apply_time/refund_reason/refund_proof_urls`
- 卖家/管理员同意：`refund_status=2`，写入 `refund_decision_time/refund_decision_user_id/refund_decision_remark`
- 卖家/管理员拒绝：`refund_status=3`，写入 `refund_decision_time/refund_decision_user_id/refund_decision_remark`

### 前端展示规则（简述）

- **订单详情页/售后页**：在“售后进度”标题旁展示“平台已同意/拒绝：审核意见”
- **后台订单详情 Drawer**：展示审核人（昵称/用户名 + ID）、审核意见，并在售后时间线终止节点附带简短审核意见

