# UC05 需求—测试追踪

状态：后端集成与 API 证据已完成；浏览器 spec 已完成但真实 Compose 执行未完成。

| 场景 | 后端测试 | 前端真实 E2E | 状态 |
| --- | --- | --- | --- |
| 举报提交与本人查询 | `ReportBlockCreditUc05IntegrationTest` | `uc05-governance.spec.ts` | 后端已完成；浏览器待运行 |
| 拉黑、双向查询、取消拉黑 | `ReportBlockCreditUc05IntegrationTest` | `uc05-governance.spec.ts` | 后端已完成；浏览器待运行 |
| 审核、扣分、信用流水、管理员审计一致 | `ReportBlockCreditUc05IntegrationTest` | `uc05-governance.spec.ts` | 后端已完成；浏览器待运行 |
| 重复审核、重复拉黑、自操作、非管理员拒绝 | `ReportBlockServiceImplTest` + 集成测试 | `uc05-governance.spec.ts` | 已完成 |
| API 控制器契约 | `ReportBlockControllerWebMvcTest` | 由真实 API 调用覆盖 | 已完成 |

WebMvc/H2 集成证据与 Docker Compose 浏览器证据分开记录，不将 MockMvc 结果冒充真实 E2E。
