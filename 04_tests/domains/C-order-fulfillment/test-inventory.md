# Domain-C 测试统计边界

| 分类 | 计入 Domain-C | 计入具体 UC | 说明 |
|---|---:|---:|---|
| `DOMAIN_C + UC11..UC15` | 是 | 是 | 对应 UC 的真实自动化验收证据 |
| `DOMAIN_C + PLATFORM` | 是 | 否 | 幂等、状态机、资金结算等共享规则 |
| Mock 控制器测试 | 是 | 仅作辅助 | 不得替代数据库 Integration |
| 二手订单测试 | 否 | 否 | 不得替代 UC13 新品订单履约 |
| Playwright real-mode | 是 | 是 | 必须连接真实后端并刷新或重新查询验证持久化 |
| 文档计划 | 否 | 否 | 只有实际执行报告可以标记 PASS |

UC11-UC15 的数量以各自 `04_tests/UCxx/evidence/result-summary.json` 和 Playwright 报告为准，不以测试方法数衡量完成度。
