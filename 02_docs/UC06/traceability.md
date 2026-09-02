# UC06 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ06 / UC06 搜索筛选详情 | SYS-BEH06 / CONCEPT-CLASS06 / COMP-STRUCT06 / COMP-SEQ06 / DESIGN-CLASS06 / OBJ-SEQ06 | `ProductListView.vue`、`ProductDetailView.vue`；`CatalogController`、`CatalogService` | E2E-TC06-001 `t0601_combinedSearchAndPublicDetail`；E2E-TC06-002 `t0602_filtersSortsEmptyAndExceptionPaths`；E2E-TC06-001 `frontend/e2e/domain-b/uc06-catalog.spec.ts` | **CI_E2E_PASS / ACTIONS_ARTIFACT**：服务/API 原始结果已归档；最新 main 的全 UC Playwright Job 通过（https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696），完整 Playwright 报告由 Actions artifact 保存，Git 仅保留结构化摘要和关键证据。 |

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
- 浏览器测试：`frontend/e2e/domain-b/uc06-catalog.spec.ts`
- 原始证据：`../../04_tests/UC06/evidence/`
