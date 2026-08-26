# UC11 购物车结算与创建订单 API 测试计划

## 测试目标

验证 `POST /api/order/create` 的身份与参数校验、服务端计价、库存和优惠券事务一致性，以及幂等提交。

## 当前证据

- `IdempotencyInterceptorTest.shouldAllowFirstRequestAndReplayDuplicateResult` 覆盖通用幂等回放。
- 当前没有直接覆盖 `OrderServiceImpl#createOrder` 或创建订单 HTTP 合约的自动化测试，本计划中的用例不得标记为已通过。

## 计划用例

| 编号 | 层级 | 场景 | 核心断言 |
|---|---|---|---|
| `UT-UC11-001` | 单元 | 合法商品、本人地址、无优惠券创建订单 | 待付款主记录和合并后明细正确 |
| `UT-UC11-002` | 单元 | 重复商品项与服务端计价 | 数量合并且不信任前端价格 |
| `UT-UC11-003` | 单元 | 下架/缺货/地址越权/本人商品/屏蔽关系 | 明确拒绝且无持久化副作用 |
| `UT-UC11-004` | 单元 | 合法与非法优惠券 | 折扣及平台/商家承担额正确 |
| `API-UC11-001` | API | JWT 创建订单主路径 | 响应、订单主从表与库存一致 |
| `API-UC11-002` | API | 后续商品缺货 | 整个事务回滚，券未占用 |
| `API-UC11-003` | API | 相同幂等键重复提交 | 只建一单并只扣一次库存 |
| `E2E-UC11-001` | E2E | 购物车结算到订单详情 | 页面、网络响应和刷新后状态一致 |

## 验证命令

```powershell
cd backend
mvn '-Dtest=IdempotencyInterceptorTest' test
```

该命令只回归当前已有幂等测试；新增用例实现后，应将对应测试类加入 `-Dtest` 并保存 Surefire XML。
