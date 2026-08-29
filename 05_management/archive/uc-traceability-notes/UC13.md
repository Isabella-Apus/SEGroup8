# UC13 追溯矩阵

| 需求/验收标准 | 设计 | 代码入口 | 已有后端证据 | 浏览器状态 |
| --- | --- | --- | --- | --- |
| `REQ13`, `AC13-01` 卖家发货 | [system.mmd](system.mmd), [component.mmd](component.mmd) | `OrderController`, `OrderServiceImpl` | `04_tests/UC13/evidence/result-summary.json`：4 条后端测试通过 | `E2E_PENDING`，本轮不处理 |
| `AC13-02` 生成/查询物流轨迹 | [object.mmd](object.mmd) | `LogisticsServiceImpl` | `UC13-订单发货物流与收货-测试报告.md` | `E2E_PENDING` |
| `AC13-03` 买家确认收货 | [system.mmd](system.mmd) | `OrderServiceImpl` | Domain-C Surefire 证据 | `E2E_PENDING` |
| `AC13-04` 越权和重复履约被拒绝/幂等 | [component.mmd](component.mmd) | 归属、状态与物流校验 | `04_tests/domains/C-order-fulfillment/evidence/result-summary.json` | `E2E_PENDING` |

完整需求见 [UC13 需求](../../01_requirements/UC13-订单发货物流与收货.md)，完整测试说明见 [UC13 测试报告](../../04_tests/UC13/UC13-订单发货物流与收货-测试报告.md)。
