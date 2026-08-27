# UC05 测试报告

结论：UC05 后端/API/H2 集成、Playwright spec、报告、追溯和真实 Compose + MySQL
Chromium 浏览器验收均已完成。

后端集成测试覆盖：举报/查询、单向拉黑与双向可见性、取消拉黑幂等错误、管理员审核扣分、`credit_score_log` 与 `admin_audit_log` 一致性、重复审核、自举报、自拉黑和非管理员拒绝。

当前 H2、MockMvc 和真实 Compose/MySQL/Chromium 结果均写入 `evidence/result-summary.json`，并分别保留对应 Evidence。

## 完成项

- 已完成举报/本人查询、拉黑双向可见性与解除幂等、管理员审核、信用分扣减、
  `credit_score_log`、`admin_audit_log` 一致性，以及重复/自操作/非管理员拒绝的
  H2 集成和 API 契约覆盖。
- 已完成 save-epicA-changes 中 Report/Block Controller/Service API 覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc05-governance.spec.ts`：举报、拉黑、管理员审核、
  信用分回读、权限异常和页面刷新持久化。
- 已完成 `02_docs/UC05/traceability.md`、本报告和 `result-summary.json` 更新。
- 已完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots 目录（本次通过无失败截图）。

## PR 所需信息

- CI：`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`
- 修改文件：`backend/src/test/java/com/segroup8/platform/controller/ReportBlockControllerWebMvcTest.java`、
  `backend/src/test/java/com/segroup8/platform/service/impl/ReportBlockServiceImplTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/ReportBlockCreditUc05IntegrationTest.java`、
  `frontend/e2e/domain-a/uc05-governance.spec.ts`、`04_tests/UC05/test-plan.md`、
  `04_tests/UC05/test-report.md`、`04_tests/UC05/evidence/result-summary.json`、
  `02_docs/UC05/traceability.md`。
- Evidence：`04_tests/UC05/evidence/`；`raw-reports/` 已提交本次 18 个后端测试的
  Surefire XML/TXT，`logs/`、`playwright-report/`、`playwright-results.json` 和
  `test-results/.last-run.json` 已提交真实 Compose 浏览器运行产物。
- 风险：本地 Docker 使用了缓存镜像别名以绕过 Docker Hub 证书问题；CI 仍需能够拉取
  workflow 中声明的上游镜像。H2/MockMvc 与真实 MySQL/Chromium 证据不能互相替代。
