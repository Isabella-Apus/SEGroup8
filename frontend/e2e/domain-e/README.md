# Domain-E Playwright specs

UC21–UC25 的真实浏览器 spec 放在本目录，必须直接使用团队共享脚手架：

```ts
import { test, expect } from "../fixtures";
```

文件名固定为：

- `uc21-voucher-lifecycle.spec.ts`
- `uc22-claim-checkout.spec.ts`
- `uc23-wallet-settlement.spec.ts`
- `uc24-chat.spec.ts`
- `uc25-notification.spec.ts`

共享 Playwright/Compose 已由 PR #133 合并。禁止复制新的 config、fixture 或 Compose。`ci-cd.yml` 的 `all-e2e-tests` 会直接运行本目录；CI 会先检查每个 Domain 至少存在一个 `.spec.ts`，避免空目录被误报为通过。

默认运行结果写入共享目录 `04_tests/platform-e2e/evidence/`。单独验收某个用例时，同时设置 `E2E_EVIDENCE_ROOT=04_tests/UCxx/evidence` 和 `E2E_OUTPUT_DIR=04_tests/UCxx/evidence/raw-reports/playwright`，再通过统一脚本运行对应 spec。只有真实 Compose 执行完成并保存报告、截图和日志后，才能把该用例记为 PASS。
