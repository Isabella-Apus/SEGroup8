# UC15 追溯矩阵

| 需求/验收标准 | 设计 | 代码入口 | 已有后端证据 | 浏览器状态 |
| --- | --- | --- | --- | --- |
| `REQ15`, `AC15-01` 已完成订单评价 | [system.mmd](system.mmd), [component.mmd](component.mmd) | `ReviewController`、review service | `04_tests/UC15/evidence/result-summary.json`：9 条后端测试通过 | `E2E_PENDING`，本轮不处理 |
| `AC15-02` 评价归属、评分和内容校验 | [object.mmd](object.mmd) | review 校验与 mapper | `ReviewServiceTest`, `ReviewFlowIntegrationTest` | `E2E_PENDING` |
| `AC15-03` 允许时追评 | [system.mmd](system.mmd) | `ReviewController` 追评分支 | UC15 MySQL evidence | `E2E_PENDING` |
| `AC15-04` 卖家仅回复本人商品评价 | [component.mmd](component.mmd) | reply ownership check | `ReviewControllerWebMvcTest` | `E2E_PENDING` |

完整需求见 [UC15 需求](../../01_requirements/UC15-订单评价追评与回复.md)，完整测试说明见 [UC15 测试报告](../../04_tests/UC15/UC15-订单评价追评与回复-测试报告.md)。
