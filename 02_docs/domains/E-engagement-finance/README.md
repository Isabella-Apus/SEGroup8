# E-engagement-finance 设计索引

本目录是 Domain-E 的共享索引，不集中存放五个 UC 的需求或六类图模型。

| UC | 父 Issue | Day3–Day5 Task | 设计目录 |
|---|---:|---:|---|
| UC21 | #60 | #142 | `02_docs/UC21/` |
| UC22 | #61 | #143 | `02_docs/UC22/` |
| UC23 | #62 | #144 | `02_docs/UC23/` |
| UC24 | #63 | #145 | `02_docs/UC24/` |
| UC25 | #64 | #146 | `02_docs/UC25/` |

历史组件级证据为 #90–#93、#95。新 PR 应通过索引或 `git mv` 整理旧图，避免产生两份真相。

共享 Playwright/Compose 依赖已经由 PR #133 合并；各 UC 直接接入 `frontend/e2e/domain-e/`。
