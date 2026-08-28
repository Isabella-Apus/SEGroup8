# UC09 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ09 / UC09 商品风险审核 | SYS-BEH09 / CONCEPT-CLASS09 / COMP-STRUCT09 / COMP-SEQ09 / DESIGN-CLASS09 / OBJ-SEQ09 | `AdminProductRiskAuditView.vue`；`ProductRiskAuditServiceImpl` | E2E-TC09-001~03 `RiskApiAndE2ETest`；E2E-TC09-001 `frontend/e2e/domain-b/uc09-risk-audit.spec.ts` | **CI_E2E_PASS / LOCAL_ARTIFACT_MISSING**：服务/API 原始结果已归档；最新 main 的全 UC Playwright Job 通过（https://github.com/Isabella-Apus/SEGroup8/actions/runs/33185345952/job/98897601611），但仓库未保存本用例独立 Playwright JSON。 |

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
- 浏览器测试：`frontend/e2e/domain-b/uc09-risk-audit.spec.ts`
- 原始证据：`../../04_tests/UC09/evidence/`
