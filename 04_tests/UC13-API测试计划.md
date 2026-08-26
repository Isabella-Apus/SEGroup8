# UC13 订单发货、物流与收货 API 测试计划

## 测试目标

验证新品订单发货、物流模板与轨迹、查询权限、人工/自动确认收货和资金结算幂等。

## 当前证据

- `OrderServiceImplTest` 覆盖发货通知和遗留合并订单拒绝。
- `OrderSettlementRefundFlowIntegrationTest` 覆盖自动确认与账户隔离。
- `SecondhandOrderFlowIntegrationTest` 只提供共享物流组件的邻近覆盖，不能替代新品闭环。

## 计划用例

| 编号 | 层级 | 场景 | 核心断言 |
|---|---|---|---|
| `UT-UC13-001` | 单元 | 发货状态与卖家权限 | 非卖家/非待发货拒绝 |
| `UT-UC13-002/003` | 单元 | 路径生成、终点和重复发货 | 轨迹有序且不重复 |
| `API-UC13-001` | API | 新品订单发货 | 待收货状态和首条已揽收轨迹 |
| `API-UC13-002` | API | 推进与查询权限 | 买卖双方可见，无关用户 403 |
| `API-UC13-003` | API | 买家确认收货 | 待评价、余额和流水只结算一次 |
| `API-UC13-004` | API | 人工/自动确认竞态 | 只有一次条件更新与结算成功 |
| `API-UC13-005` | API | 遗留合并订单 | 返回 409 且订单/物流表不变 |
| `E2E-UC13-001` | E2E | 卖家发货到买家收货 | 双方页面与物流时间线一致 |

## 验证命令

```powershell
cd backend
mvn '-Dtest=OrderServiceImplTest,OrderSettlementRefundFlowIntegrationTest,SecondhandOrderFlowIntegrationTest' test
```

验收新品 UC13 时必须补充独立 API 闭环，不能以二手 UC20 用例代替。
