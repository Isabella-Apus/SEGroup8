# UC12 追溯矩阵

| 需求/验收标准 | 设计 | 代码入口 | 已有后端证据 | 浏览器状态 |
| --- | --- | --- | --- | --- |
| `REQ12`, `AC12-01` 支付待付款订单 | [system.mmd](system.mmd), [component.mmd](component.mmd) | `OrderController`, `OrderServiceImpl` | `04_tests/UC12/evidence/result-summary.json`：8 条后端测试通过 | `E2E_PENDING`，本轮不处理 |
| `AC12-02` 支付后状态和金额一致 | [object.mmd](object.mmd) | `order_info`, `order_item`、支付服务 | `04_tests/UC12/` MySQL/集成报告 | `E2E_PENDING` |
| `AC12-03` 合法取消并保持幂等 | [system.mmd](system.mmd) | `OrderServiceImpl` 取消分支 | `UC12-订单支付与取消-测试报告.md` | `E2E_PENDING` |
| `AC12-04` 越权/非法状态拒绝且不改库 | [component.mmd](component.mmd) | 归属和状态校验 | `04_tests/domains/C-order-fulfillment/evidence/result-summary.json` | `E2E_PENDING` |

完整需求见 [UC12 需求](../../01_requirements/UC12-订单支付与取消.md)，完整测试说明见 [UC12 测试报告](../../04_tests/UC12/UC12-订单支付与取消-测试报告.md)。
