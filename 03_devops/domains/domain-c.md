# Domain-C 测试与交付规范

## 本地入口

后端共享定向测试：

```powershell
./04_tests/domains/C-order-fulfillment/run-domain-c-tests.ps1 -Suite DOMAIN_C
```

单个 UC 使用 `-Suite UC11` 至 `-Suite UC15`。运行器根据 JUnit 标签筛选，执行失败、零测试或 Evidence 不完整时返回非零退出码。

真实浏览器测试：

```powershell
$env:COMPOSE_FILE = "compose.yml;04_tests/domains/C-order-fulfillment/compose.e2e.yml"
$env:E2E_BASE_URL = "http://127.0.0.1:8088"
$env:E2E_USERNAME = "user"
$env:E2E_PASSWORD = "user123"
$env:E2E_ROLE = "USER"
$env:DOMAIN_C_SUITE = "domains/C-order-fulfillment"
$env:E2E_EVIDENCE_ROOT = "04_tests/domains/C-order-fulfillment/evidence"
$env:E2E_OUTPUT_DIR = "04_tests/domains/C-order-fulfillment/evidence/raw-reports/playwright"
./scripts/e2e/run-compose-e2e.ps1 -ResetDatabase e2e/domain-c
```

测试页面地址为 `http://127.0.0.1:8088`，后端地址为 `http://127.0.0.1:8089`。命令复用平台统一 E2E 启动器；Domain-C Compose override 仅提供独立网络和 `segroup8-domain-c-e2e-mysql-data` 数据卷，不复制启动、健康检查或失败退出逻辑。

启动器默认会清理容器。使用 `-KeepServices` 调试后，在仓库根目录执行：

```powershell
docker compose -f compose.yml -f 04_tests/domains/C-order-fulfillment/compose.e2e.yml down -v
```

## CI 边界

`.github/workflows/domain-c.yml` 包含相互独立的后端定向测试和真实浏览器 Job。后端上传 Maven 日志、Surefire XML/TXT 和汇总 JSON；E2E Job 使用 Compose 启动 MySQL、单体后端和 real-mode 前端，并上传 Playwright 报告、截图和 Compose 日志。

Domain-C 浏览器 Job 复用主线的 `scripts/e2e/run-compose-e2e.*`、`frontend/playwright.config.ts` 和平台 E2E helper。`E2E_EVIDENCE_ROOT` 定向运行日志，`E2E_OUTPUT_DIR` 定向 Playwright 原始报告；CI 在执行前清理这两类浏览器证据，避免仓库内旧报告造成假通过。

## 分支顺序

1. `test/domain-c-infra` 必须从最新 `origin/main` 创建并先合并。
2. 每个 `test/ucxx-real-flow` 必须在前一 PR 合并并更新 `main` 后重新从最新主线创建。
3. UC 分支之间不得互相作为基线，也不得把五条业务 E2E 集中到基础设施 PR。

Day3-Day5 Task 只有在 Integration、真实 E2E、Evidence、CI 和非作者 Review 全部完成后才可关闭；UC11-UC15 父 Issue 继续跟踪 Day6-Day8 与 Final 追溯。
