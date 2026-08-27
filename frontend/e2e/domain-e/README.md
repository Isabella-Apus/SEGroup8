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

共享 Playwright/Compose 已由 PR #133 合并。禁止复制新的 config、fixture 或 Compose。默认运行结果写入共享目录 `04_tests/platform-e2e/evidence/`；若要为单个 UC 留存独立证据，应显式设置 `E2E_OUTPUT_DIR=../04_tests/UCxx/evidence` 后再运行对应 spec。每个 spec 必须在真实 Compose 环境实际运行并保存证据后，才能报告 PASS。
