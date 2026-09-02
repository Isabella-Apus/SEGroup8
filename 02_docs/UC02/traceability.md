# UC02 追溯矩阵

| 需求 / 用例 | 六类图模型 | 主要代码模块 | 测试编号与现有测试 | 结果 / 证据 |
|---|---|---|---|---|
| REQ02 / UC02 资料和地址 | SYS-BEH02 / CONCEPT-CLASS02 / COMP-STRUCT02 / COMP-SEQ02 / DESIGN-CLASS02 / OBJ-SEQ02 | `Profile.vue`、`AddressManager.vue`；`UserController`、`UserServiceImpl`、`AddressMapper` | UNIT-TC02-001 `UserServiceImplTest` 3 项，含默认地址和越权删除；E2E-TC02-001 `frontend/e2e/domain-a/uc02-profile-address.spec.ts` | **MAIN_CI_E2E_PASS**：保留结构化结果，并由当前 main 完整系统流水线复验，1/1 通过、unexpected 0；路径 `../../04_tests/UC02/evidence/playwright-results.json`。最新 main 全量 E2E CI 同时通过：https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696 |

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
- 浏览器测试：`frontend/e2e/domain-a/uc02-profile-address.spec.ts`
- 原始证据：`../../04_tests/UC02/evidence/`
