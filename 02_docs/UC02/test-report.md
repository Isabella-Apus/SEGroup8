# UC02 测试报告

结论：UC02 后端/API/H2 集成、Playwright spec、报告、追溯和真实 Compose + MySQL
Chromium 浏览器验收均已完成。

The H2-backed Spring integration gate passed on 2026-08-27: 2 tests, 0
failures, 0 errors. It verified persisted profile changes, two default
address writes collapsing to one default, CRUD re-query, cross-user rejection
and unauthenticated rejection.

The real Compose + MySQL + Chromium gate also passed locally on 2026-08-27:

- `e2e/domain-a/uc02-profile-address.spec.ts`: 1 test passed, 0 failures.
- Evidence was written to `04_tests/UC02/evidence/`, including Playwright
  HTML/JSON output and Compose service logs.

Saved API coverage in `UserControllerWebMvcTest` passed with 8 tests, including
the `/me`, profile update and address update contracts.

H2 evidence remains separately reported; this run is the real MySQL/browser
evidence for UC02.

## 完成项

- 已完成资料回读、地址 CRUD、默认地址唯一、删除不可见、跨用户越权和未登录拒绝
  的真实 H2 集成链。
- 已完成 save-epicA-changes 中 User Controller API 覆盖的分流和回归。
- 已完成 `frontend/e2e/domain-a/uc02-profile-address.spec.ts`：资料和地址主链、
  默认地址断言、删除后刷新不可见、跨用户更新/删除拒绝。
- 已完成 `02_docs/UC02/traceability.md`、本报告和 `result-summary.json` 更新。
- 已完成：在真实 Docker Compose frontend/backend/MySQL 上执行浏览器 spec，并提交
  Playwright raw report、logs 和 screenshots 目录（本次通过无失败截图）。

## PR 所需信息

- CI：`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`
- 修改文件：`backend/src/test/java/com/segroup8/platform/controller/UserControllerWebMvcTest.java`、
  `backend/src/test/java/com/segroup8/platform/integration/ProfileAddressUc02IntegrationTest.java`、
  `frontend/e2e/domain-a/uc02-profile-address.spec.ts`、`02_docs/UC02/test-plan.md`、
  `02_docs/UC02/test-report.md`、`04_tests/UC02/evidence/result-summary.json`、
  `02_docs/UC02/traceability.md`。
- Evidence：`04_tests/UC02/evidence/`；`raw-reports/` 已提交本次 10 个后端测试的
  Surefire XML/TXT，`logs/`、`playwright-report/`、`playwright-results.json` 和
  `test-results/.last-run.json` 已提交真实 Compose 浏览器运行产物。
- 风险：本地 Docker 使用了缓存镜像别名以绕过 Docker Hub 证书问题；CI 仍需能够拉取
  workflow 中声明的上游镜像。地址字段契约变化时需同步 E2E 断言。
