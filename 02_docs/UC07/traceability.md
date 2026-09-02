# UC07 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ07 / UC07 卖家商品生命周期 | SYS-BEH07 / CONCEPT-CLASS07 / COMP-STRUCT07 / COMP-SEQ07 / DESIGN-CLASS07 / OBJ-SEQ07 | `SellerProductList.vue`、`SellerProductEdit.vue`；`ProductController`、`ProductServiceImpl` | E2E-TC07-001~04 `CatalogApiAndE2ETest` 的 UC07 场景；UNIT-TC07-001 `ProductServiceImplTest`；E2E-TC07-001 `frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts` | **CI_E2E_PASS / ACTIONS_ARTIFACT**：服务/API 原始结果已归档；最新 main 的全 UC Playwright Job 通过（https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696），完整 Playwright 报告由 Actions artifact 保存，Git 仅保留结构化摘要和关键证据。 |

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
- 浏览器测试：`frontend/e2e/domain-b/uc07-product-lifecycle.spec.ts`
- 原始证据：`../../04_tests/UC07/evidence/`
