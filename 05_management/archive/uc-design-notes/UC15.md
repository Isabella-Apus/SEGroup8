# UC15 订单评价、追评与回复

- 需求：`REQ15 / UC15`
- 所属领域：Domain C（Order Fulfillment）
- 本目录是 UC15 的标准嵌套文档入口。

## 文档导航

- [需求](../../01_requirements/UC15-订单评价追评与回复.md)
- [系统级图](system.mmd)
- [组件级图](component.mmd)
- [对象级图](object.mmd)
- [追溯矩阵](traceability.md)
- [后端测试报告](../../04_tests/UC15/UC15-订单评价追评与回复-测试报告.md)

## 业务与设计边界

买家对已完成且本人购买的订单发表评价，并在规则允许时追评；卖家只能回复本人商品对应评价。当前实现中追评、回复和查询仍直接由 `ReviewController` 及现有 review 服务/持久化链路承接，文档不虚构不存在的服务拆分。

| 组件 | 责任 |
| --- | --- |
| `ReviewController` | 评价、追评、回复和查询接口。 |
| Review service/mapper | 校验订单完成状态、用户归属、评价时限和回复归属。 |
| 订单与评价表 | 保存评价内容、评分、追评、回复和时间。 |

## 验证口径

`04_tests/UC15/evidence/result-summary.json` 记录 9 条后端 MySQL/集成测试通过；已有浏览器产物为历史记录，本轮不重新执行 Playwright E2E。
