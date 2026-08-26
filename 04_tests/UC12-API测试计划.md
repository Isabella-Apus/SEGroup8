# UC12 订单支付与取消 API 测试计划

## 测试目标

验证支付/取消状态机、支付拆单金额守恒、余额与流水、库存与优惠券回滚，以及重复/并发请求无二次副作用。

## 当前证据

- `OrderServiceImplTest.payMyOrder_shouldSplitCartItemsIntoIndependentPaidOrders` 覆盖支付拆分主路径。
- `OrderServiceImplTest.cancelMyOrder_shouldRestoreStockWhenUnpaidEvenIfOrderStatusNotPendingPay` 覆盖未支付取消回补库存。

## 计划用例

| 编号 | 层级 | 场景 | 核心断言 |
|---|---|---|---|
| `UT-UC12-001` | 单元 | 支付状态允许/拒绝矩阵 | 仅待付款可支付 |
| `UT-UC12-002` | 单元 | 取消状态允许/拒绝矩阵 | 已完成/已关闭不可取消 |
| `UT-UC12-003` | 单元 | 商城币余额不足 | 订单、余额和流水均不变 |
| `UT-UC12-004` | 单元 | 未支付与已支付取消 | 只对未支付订单回补库存和释放券 |
| `API-UC12-001` | API | 支付与按商品拆单 | 金额、优惠分摊、状态和版本守恒 |
| `API-UC12-002` | API | 取消未支付订单 | 订单关闭、库存恢复、优惠券释放 |
| `API-UC12-003` | API | 并发/重复支付与取消 | 最多一次成功且无重复副作用 |
| `API-UC12-004` | API | 非买家操作 | 返回 403 且数据不变 |
| `E2E-UC12-001/002` | E2E | 支付或取消后刷新 | 买家和卖家页面状态一致 |

## 验证命令

```powershell
cd backend
mvn '-Dtest=OrderServiceImplTest' test
```

当前命令包含 UC12 已有服务测试；新增 API 测试实现后需追加相应 WebMvc/集成测试类。
