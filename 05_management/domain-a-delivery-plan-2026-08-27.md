# Domain A delivery and review map

This file is the execution checklist for the six PRs required by
`day3MAprompt.md`. The remote GitHub CLI is not installed in this workspace, so
Task Issue creation, Project card placement, and PR creation remain manual
GitHub operations. All six code branches have been pushed.

本轮不按 prompt 中的 Day3/Day4/Day5 时间段拆分；目标是一次完成 prompt
规定的完整交付集。当前分支按一个 A0 加五个 UC 独立 PR 维护，save-epicA-changes
保持不改动。

CI workflow：
`https://github.com/Isabella-Apus/SEGroup8/actions/workflows/ci-cd.yml`

## Parent issues

- Epic A: `#34`
- UC01 parent: `#67`
- UC02 parent: `#40`
- UC03 parent: `#41`
- UC04 parent: `#42`
- UC05 parent: `#43`

Keep the UC parent issues open until their later service-stage gaps are closed.
Each new Task issue below is a child of the corresponding parent and should be
the only issue closed by its PR.

## Task issues

| Task title | Parent | Branch | PR subject | Evidence |
| --- | --- | --- | --- | --- |
| `[EPIC-A] 测试去重、JUnit 标签、领域报告和 CI 命令整理` | #34 | `test/domain-a-infra` | `test(domain-a): normalize testing infrastructure and evidence` | `04_tests/domains/A-identity` |
| `[UC01] 注册登录、JWT、角色和封禁联动真实集成/E2E` | #67 | `test/uc01-real-flow` | `test(UC01): complete real auth identity flow` | `04_tests/UC01` |
| `[UC02] 用户资料、地址归属和默认地址真实集成/E2E` | #40 | `test/uc02-real-flow` | `test(UC02): complete real profile and address flow` | `04_tests/UC02` |
| `[UC03] 商家申请、审核、角色升级和审计真实集成/E2E` | #41 | `test/uc03-real-flow` | `test(UC03): complete real merchant application flow` | `04_tests/UC03` |
| `[UC04] 封禁、登录失败、解禁和审计真实集成/E2E` | #42 | `test/uc04-real-flow` | `test(UC04): complete real ban unban governance flow` | `04_tests/UC04` |
| `[UC05] 举报、拉黑、信用治理和审计事务 E2E` | #43 | `test/uc05-real-flow` | `test(UC05): complete report block credit governance flow` | `04_tests/UC05` |

## save-epicA-changes 测试分流记录

| save 分支内容 | 归属 | 落点 |
|---|---|---|
| `JwtAuthInterceptorTest`；`JwtUtilsTest` 的 PLATFORM 边界；`security-contract`（`JwtPrincipal`、`JwtTokenVerifier`、异常类及 5 个契约测试） | 共享 A0 / 全局安全 | `backend/src/test/.../interceptor`、`microservices/security-contract`、`02_docs/domains/A-identity`、`03_devops/domains/domain-a.md` |
| Auth Controller/Service 新增的非法请求、重复用户名、错误密码、封禁登录覆盖 | UC01 | `test/uc01-real-flow` 的 `AuthControllerWebMvcTest.java`、`AuthServiceImplTest.java`，并与 `IdentityUc01IntegrationTest`、`uc01-auth.spec.ts` 同分支 |
| User Controller 的当前用户、资料和地址 API 覆盖 | UC02 | `test/uc02-real-flow` 的 `UserControllerWebMvcTest.java`，并与 `ProfileAddressUc02IntegrationTest`、`uc02-profile-address.spec.ts` 同分支 |
| Merchant Application Controller/Service 的待审队列、重复申请、驳回原因/通知覆盖 | UC03 | `test/uc03-real-flow` 的 `MerchantApplicationControllerWebMvcTest.java`、`MerchantApplicationServiceImplTest.java`，并与两个 UC03 集成测试、E2E 同分支 |
| Admin User Controller/Service 的列表、审计、解禁和自封禁拒绝覆盖 | UC04 | `test/uc04-real-flow` 的 `AdminUserControllerWebMvcTest.java`、`AdminUserServiceImplTest.java`，并与 `UserGovernanceUc04IntegrationTest`、E2E 同分支 |
| Report/Block Controller/Service 的举报、拉黑、解除、信用、管理员、重复/自操作/越权覆盖 | UC05 | `test/uc05-real-flow` 的 `ReportBlockControllerWebMvcTest.java`、`ReportBlockServiceImplTest.java`，并与 `ReportBlockCreditUc05IntegrationTest`、E2E 同分支 |

save 分支中 frontend 旧的 mock/fixture 删除改动没有复用；因为当前 Playwright
脚手架的真实登录选择器和 helper 需要保留。生成的 `target` 及非 Domain-A
微服务目录/风险/店铺测试也没有带入这六个 PR。

## Issue body checklist

Copy the following sections into each Task issue and fill in the branch/PR
link:

1. Gap: the missing real DB chain, API contract coverage, refresh persistence,
   or evidence boundary described by the matching UC README.
2. Files: the matching `01_requirements`, `02_docs`, `04_tests`, backend
   integration/API tests, and frontend E2E spec.
3. Acceptance: happy path, core permission/error path, persisted re-query after
   refresh, and evidence/report update.
4. Local commands: `mvn -B -f backend/pom.xml -Dgroups=DOMAIN_A test`,
   `mvn -B -f backend/pom.xml clean verify`, `npm ci`,
   `npm run build:real`, and the Compose E2E runner.
5. Risk: H2/MockMvc is not MySQL/browser evidence; Docker daemon availability
   and seed credentials must be recorded.
6. Evidence directory and final PR URL.

当前已完成的本地证据：A0 基线 Domain-A 33 tests、后端 `clean verify` 95 tests，
五个 UC 合并后的最终本地聚合 Domain-A 65 tests、后端全量 127 tests，均 PASS；
security-contract 5 tests、frontend `npm ci`/`npm run build:real` 和 Compose
配置检查均 PASS；`04_tests/UC01` 至 `UC05` 的 `result-summary.json` 已明确
记录各自 H2/MockMvc PASS 以及 Compose 浏览器 `NOT_RUN`。Docker daemon 恢复后，
必须把 Playwright raw report、logs 和 screenshots 写回对应 UC evidence 目录。

## PR rules

- Create the PR from the listed branch after confirming it is up to date with
  the intended `main` base.
- Use `Closes <Task-Issue-ID>` only when code, integration test, browser E2E,
  report, and evidence are all complete. Otherwise use `Refs <Task-Issue-ID>`.
- Do not close the UC parent issue from these six PRs.
- Add the Task issue and its PR to the shared Project under Epic A, and link
  the UC parent in the issue hierarchy.
- The current evidence deliberately records Compose browser E2E as `NOT_RUN`
  because Docker Desktop's Linux daemon was unavailable during this run.

## Current branch heads before report refresh

The pushed independent branch heads before this report/CI refresh were:

- A0 `test/domain-a-infra` → `800c3d1`
- UC01 `test/uc01-real-flow` → `f5fd8d4`
- UC02 `test/uc02-real-flow` → `85ac3ea`
- UC03 `test/uc03-real-flow` → `d19e0f9`
- UC04 `test/uc04-real-flow` → `6973b9e`
- UC05 `test/uc05-real-flow` → `b3bf285`

报告/CI 更新完成后会在各分支产生新的独立提交并重新推送，PR 以新的远端头为准。

## Current local heads after report refresh

- A0 `test/domain-a-infra` → `776d1f1`
- UC01 `test/uc01-real-flow` → `06175f3`
- UC02 `test/uc02-real-flow` → `a210744`
- UC03 `test/uc03-real-flow` → `f49618d`
- UC04 `test/uc04-real-flow` → `347e5e5`
- UC05 `test/uc05-real-flow` → `67d1ab8`

由于远端认证失败，这些新提交当前只在本地；认证恢复后按上述分支逐一
`git push origin <branch>`，再按 A0 → UC01 → UC02 → UC03 → UC04 → UC05
的顺序建立/合并 PR。
