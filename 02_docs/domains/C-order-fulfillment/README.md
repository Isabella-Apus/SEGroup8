# Domain C：订单履约

Domain C 对应 UC11–UC15。每个用例现在都有独立需求入口；UC11–UC15 的设计、三层图和追溯矩阵统一从 `02_docs/UCxx/` 进入。

| 用例 | 需求 | 标准设计目录 | 追溯 |
| --- | --- | --- | --- |
| UC11 | [购物车结算与创建订单](../../../01_requirements/UC11-购物车结算与创建订单.md) | [UC11](../../UC11/README.md) | [追溯](../../UC11/traceability.md) |
| UC12 | [订单支付与取消](../../../01_requirements/UC12-订单支付与取消.md) | [UC12](../../UC12/README.md) | [追溯](../../UC12/traceability.md) |
| UC13 | [订单发货物流与收货](../../../01_requirements/UC13-订单发货物流与收货.md) | [UC13](../../UC13/README.md) | [追溯](../../UC13/traceability.md) |
| UC14 | [订单售后退款](../../../01_requirements/UC14-订单售后退款.md) | [UC14](../../UC14/README.md) | [追溯](../../UC14/traceability.md) |
| UC15 | [订单评价追评与回复](../../../01_requirements/UC15-订单评价追评与回复.md) | [UC15](../../UC15/README.md) | [追溯](../../UC15/traceability.md) |

## 结果口径

UC12、UC13、UC14、UC15 的结构化后端结果分别为 8、4、9、9 条通过；Domain-C 标签汇总为 19/19 通过。已有浏览器报告与当前后端结果分开保存，本轮不重跑或补充 Playwright E2E。

具体证据见 [`04_tests/domains/C-order-fulfillment/evidence/result-summary.json`](../../../04_tests/domains/C-order-fulfillment/evidence/result-summary.json) 和各 UC 测试目录。
