# UC14 追溯矩阵

| 需求/验收标准 | 设计 | 代码入口 | 已有后端证据 | 浏览器状态 |
| --- | --- | --- | --- | --- |
| `REQ14`, `AC14-01` 买家申请退款 | [system.mmd](system.mmd), [component.mmd](component.mmd) | `OrderController`, `OrderServiceImpl` | `04_tests/UC14/evidence/result-summary.json`：9 条后端测试通过 | `E2E_PENDING`，本轮不处理 |
| `AC14-02` 卖家/管理员审核 | [object.mmd](object.mmd) | `OrderServiceImpl` 审核分支 | `AdminOrderControllerWebMvcTest`、集成测试 | `E2E_PENDING` |
| `AC14-03` 退款金额与订单状态一致 | [component.mmd](component.mmd) | 退款/钱包服务 | UC14 MySQL evidence | `E2E_PENDING` |
| `AC14-04` 越权、重复和非法模式拒绝 | [system.mmd](system.mmd) | `validateRefundMode` 等校验 | Domain-C 结构化汇总 | `E2E_PENDING` |

完整需求见 [UC14 需求](../../01_requirements/UC14-订单售后退款.md)，完整测试说明见 [UC14 测试报告](../../04_tests/UC14/UC14-订单售后退款-测试报告.md)。
