# UC18 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ18 / UC18 二手议价 | SYS-BEH18 / CONCEPT-CLASS18 / COMP-STRUCT18 / COMP-SEQ18 / DESIGN-CLASS18 / OBJ-SEQ18 | `SecondhandDetailView.vue`、`ChatView.vue`；`SecondhandTradeServiceImpl` | UNIT-TC18-001 同意建单；UNIT-TC18-002 拒绝；UNIT-TC18-003 重复处理；UI-TC18-01；E2E-TC18-001 `frontend/e2e/domain-d/uc18-bargain.spec.ts` | **MAIN_CI_E2E_PASS**：保留结构化结果，并由当前 main 完整系统流水线复验，1/1 通过、unexpected 0；路径 `Actions run 33526387696 artifact`。最新 main 全量 E2E CI 同时通过：https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696 |

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
- 浏览器测试：`frontend/e2e/domain-d/uc18-bargain.spec.ts`
- 原始证据：`../../04_tests/UC18/evidence/`
