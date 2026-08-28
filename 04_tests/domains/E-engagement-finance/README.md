# Domain-E 测试报告汇总入口

## 统计边界

本目录汇总 `DOMAIN_E` 标签测试与 UC21–UC25 的实际结果。平台 smoke 单独位于 `04_tests/platform-e2e/`，不能计作 UC PASS。

## 当前状态

| 范围 | Task | 状态 |
|---|---:|---|
| 共享标签、CI、Evidence 规范 | #147 | 本 PR 建立入口 |
| UC21 | #142 | PASS：PR #197 已提供需求、Integration、真实 E2E 和 Evidence |
| UC22 | #143 | PENDING：由独立 PR 提供 |
| UC23 | #144 | PASS：本 PR 提供需求、事务修复、Integration、真实 E2E 和 Evidence |
| UC24 | #145 | PENDING：由独立 PR 提供 |
| UC25 | #146 | PENDING：由独立 PR 提供 |

## 输出要求

每个 UC 的 `result-summary.json` 必须符合本目录的 schema，并同时链接 Surefire XML、Playwright JSON/HTML、日志和截图。没有实际运行的结果必须写 `PENDING`，不得写 `PASS`。
