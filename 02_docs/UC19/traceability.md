# UC19 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ19 / UC19 二手拍卖 | SYS-BEH19 / CONCEPT-CLASS19 / COMP-STRUCT19 / COMP-SEQ19 / DESIGN-CLASS19 / OBJ-SEQ19 | `SecondhandDetailView.vue`、`SecondhandSellerView.vue`；`SecondhandTradeServiceImpl` | UNIT-TC19-001 低价；UNIT-TC19-002 结束后出价；UNIT-TC19-003 结算幂等；INT-TC19-001 历史拍卖后新建；E2E-TC19-001 `frontend/e2e/domain-d/uc19-auction.spec.ts` | **LOCAL_E2E_PASS**：已归档 Playwright JSON，1/1 通过、unexpected 0；路径 `../../04_tests/UC19/evidence/raw-reports/playwright/playwright-results.json`。最新 main 全量 E2E CI 同时通过：https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98897601611 |

## 权威材料

- 需求：[requirement.md](requirement.md)
- 系统行为模型：[system.mmd](system.mmd)
- 概念类图：[concept.mmd](concept.mmd)
- 组件结构图：[component.mmd](component.mmd)
- 组件顺序图：[component-sequence.mmd](component-sequence.mmd)
- 详细设计类图：[object.mmd](object.mmd)
- 对象顺序图：[object-sequence.mmd](object-sequence.mmd)
- 测试计划：[test-plan.md](test-plan.md)
- 测试报告：[test-report.md](test-report.md)
- 浏览器测试：`frontend/e2e/domain-d/uc19-auction.spec.ts`
- 原始证据：`../../04_tests/UC19/evidence/`
