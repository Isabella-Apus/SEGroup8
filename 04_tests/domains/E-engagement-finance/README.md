# Domain-E 测试报告汇总入口

## 统计边界

本目录汇总 `DOMAIN_E` 标签测试与 UC21–UC25 的实际结果。平台 smoke 单独位于 `04_tests/platform-e2e/`，不能计作 UC PASS。

## 当前状态

| 范围 | Task | 状态 |
|---|---:|---|
| 共享标签、CI、Evidence 规范 | #147 | 本 PR 建立入口 |
| UC21 | #142 | PASS：PR #197，证据在 `04_tests/UC21/` |
| UC22 | #143 | PASS：后端 7/7、真实 Playwright 1/1，证据在 `04_tests/UC22/` |
| UC23 | #144 | PENDING：由独立 PR 提供 |
| UC24 | #145 | PENDING：由独立 PR 提供 |
| UC25 | #146 | PENDING：由独立 PR 提供 |

## 输出要求

每个 UC 的 `result-summary.json` 必须符合本目录的 schema，并同时链接 Surefire XML、Playwright JSON/HTML、日志和截图。没有实际运行的结果必须写 `PENDING`，不得写 `PASS`。
