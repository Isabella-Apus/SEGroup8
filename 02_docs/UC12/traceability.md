# UC12 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ12 / UC12 支付取消 | SYS-BEH12 / CONCEPT-CLASS12 / COMP-STRUCT12 / COMP-SEQ12 / DESIGN-CLASS12 / OBJ-SEQ12 | `OrderView.vue`；`OrderServiceImpl`、`IdempotencyInterceptor` | UNIT-TC12-001 `cancelMyOrder_shouldRestoreStock...`；UNIT-TC12-002 `IdempotencyInterceptorTest` 3 项；E2E-TC12-001 `frontend/e2e/domain-c/uc12-pay-cancel.spec.ts` | **MAIN_CI_E2E_PASS**：保留结构化结果，并由当前 main 完整系统流水线复验，1/1 通过、unexpected 0；路径 `Actions run 33526387696 artifact`。最新 main 全量 E2E CI 同时通过：https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696 |

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
- 浏览器测试：`frontend/e2e/domain-c/uc12-pay-cancel.spec.ts`
- 原始证据：`../../04_tests/UC12/evidence/`
