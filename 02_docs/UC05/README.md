# UC05 领域文档

状态：后端/API/H2 集成与 E2E 脚本已完成；真实 Compose/MySQL 浏览器执行未完成。

UC05 的核心链路是：用户举报目标用户 → 管理员审核 → 目标用户信用分和信用流水更新，同时保留管理员审计；拉黑关系只对发起方生效，并支持双向查询和取消拉黑。

实现入口：`ReportBlockController`、`AdminReportController`、`ReportBlockServiceImpl`、`CreditServiceImpl`。

验证入口：`ReportBlockCreditUc05IntegrationTest`、`ReportBlockControllerWebMvcTest`、`ReportBlockServiceImplTest`，以及 `frontend/e2e/domain-a/uc05-governance.spec.ts`。
