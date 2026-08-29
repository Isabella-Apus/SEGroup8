# Domain-E 测试报告汇总入口

## 统计边界

本目录汇总 `DOMAIN_E` 标签测试与 UC21–UC25 的实际结果。平台 smoke 与全量 E2E 共用 `04_tests/platform-e2e/` 入口，但 smoke 不能单独计作 UC PASS。

## 当前状态

| 范围 | Task | 状态 |
|---|---:|---|
| 共享标签、CI、Evidence 规范 | #147 | 本 PR 建立入口 |
| UC21 | #142 | PASS：证据在 `04_tests/UC21/` |
| UC22 | #143 | PASS：后端与真实 Playwright 证据在 `04_tests/UC22/` |
| UC23 | #144 | PASS：后端与真实 Playwright 证据在 `04_tests/UC23/` |
| UC24 | #145 | PASS：后端与真实 Playwright 证据在 `04_tests/UC24/` |
| UC25 | #146 | PASS：后端与真实 Playwright 证据在 `04_tests/UC25/`，包含 WebSocket 断线重连补偿 |

## 输出要求

每个 UC 的 `result-summary.json` 必须符合本目录的 schema，并同时链接 Surefire XML、Playwright JSON/HTML、日志和截图。没有实际运行的结果必须写 `PENDING`，不得写 `PASS`。
