# Domain A delivery and review map

This file is the execution checklist for the six PRs required by
`day3MAprompt.md`. The remote GitHub CLI is not installed in this workspace, so
Task Issue creation, Project card placement, and PR creation remain manual
GitHub operations. All six code branches have been pushed.

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
