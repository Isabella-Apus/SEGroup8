# UC13 订单发货、物流与收货

- 需求：`REQ13 / UC13`
- 所属领域：Domain C（Order Fulfillment）
- 本目录是 UC13 的标准嵌套文档入口。

## 文档导航

- [需求](../../01_requirements/UC13-订单发货物流与收货.md)
- [系统级图](system.mmd)
- [组件级图](component.mmd)
- [对象级图](object.mmd)
- [追溯矩阵](traceability.md)
- [后端测试报告](../../04_tests/UC13/UC13-订单发货物流与收货-测试报告.md)

## 业务与设计边界

卖家只能对本人可履约订单发货，系统生成或推进物流轨迹；买家按订单归属查询物流并确认收货。发货、物流推进、收货和订单状态迁移需要保持幂等。

| 组件 | 责任 |
| --- | --- |
| `OrderController` | 发货、确认收货和订单状态接口。 |
| `LogisticsController/Service` | 创建、推进和查询物流轨迹。 |
| `OrderServiceImpl` | 校验买卖双方归属及状态迁移。 |
| `order_info`、物流表 | 持久化订单和节点状态。 |

## 验证口径

`04_tests/UC13/evidence/result-summary.json` 记录 4 条后端 MySQL/集成测试通过。已有浏览器产物作为历史记录保留；本轮不重新执行 Playwright E2E。
