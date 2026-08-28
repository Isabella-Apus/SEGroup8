# UC10 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ10 / UC10 浏览搜索热词 | SYS-BEH10 / CONCEPT-CLASS10 / COMP-STRUCT10 / COMP-SEQ10 / DESIGN-CLASS10 / OBJ-SEQ10 | `BrowseHistoryView.vue`；`SearchBehaviorServiceImpl`、`BrowseHistoryServiceImpl` | E2E-TC10-001~04 `BehaviorApiAndE2ETest`；E2E-TC10-001 `frontend/e2e/domain-b/uc10-behavior.spec.ts` | **CI_E2E_PASS / LOCAL_ARTIFACT_MISSING**：服务/API 原始结果已归档；最新 main 的全 UC Playwright Job 通过（https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98897601611），但仓库未保存本用例独立 Playwright JSON。 |

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
- 浏览器测试：`frontend/e2e/domain-b/uc10-behavior.spec.ts`
- 原始证据：`../../04_tests/UC10/evidence/`
