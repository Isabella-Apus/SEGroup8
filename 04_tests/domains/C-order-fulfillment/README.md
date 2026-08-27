# Domain-C order-fulfillment 测试入口

本目录只维护 UC11-UC15 的共享测试基础设施、统计边界和汇总 Evidence。每个 UC 的 fixture、Integration、E2E、报告和证据必须保存在 `04_tests/UCxx/`，不得以共享测试或二手订单测试替代。

## 标签规则

- 所有订单履约领域测试必须包含 `@Tag("DOMAIN_C")`。
- UC 专属测试还必须包含对应的 `@Tag("UC11")` 至 `@Tag("UC15")`。
- 不属于单个 UC 的状态机、幂等和结算基础测试使用 `@Tag("PLATFORM")`。
- `SecondhandOrderFlowIntegrationTest` 不计入 UC13 新品履约覆盖。

## 定向执行

```powershell
./04_tests/domains/C-order-fulfillment/run-domain-c-tests.ps1 -Suite DOMAIN_C
./04_tests/domains/C-order-fulfillment/run-domain-c-tests.ps1 -Suite UC11
```

跨平台入口：

```bash
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs --suite UC14 --goal verify
```

运行器默认执行 `clean verify`，收集本次 Surefire XML/TXT、完整 Maven 日志和 `result-summary.json`。汇总文件包含本次唯一 `reportSuffix`，校验器会拒绝混入其他批次的报告。Maven 失败、报告缺失、零测试或任一断言失败时均返回非零退出码。

## Evidence

共享定向测试输出到 `04_tests/domains/C-order-fulfillment/evidence/`。UC 专属输出到 `04_tests/UCxx/evidence/`，目录结构固定为：

```text
logs/
raw-reports/
  surefire/
  playwright/
screenshots/
result-summary.json
```

后端运行器只清理并重建 `raw-reports/surefire/`，不会覆盖 `raw-reports/playwright/` 中的浏览器报告。

浏览器测试复用平台唯一的 `scripts/e2e/run-compose-e2e.*`、`frontend/playwright.config.ts`、`frontend/e2e/fixtures/` 和 `frontend/e2e/helpers/`。Domain-C 仅维护隔离数据卷 override、领域 spec、成功截图和 Evidence 校验，不维护第二套 Compose 启停流程、Playwright 版本或配置。

使用 `node 04_tests/domains/C-order-fulfillment/verify-evidence.mjs --suite=UC11` 校验可提交证据。

真实浏览器证据使用以下命令校验；零测试、失败、跳过、flaky、缺少 `DOMAIN_C/UCxx` 标签、缺少 HTML 报告或缺少已附加的成功截图时返回非零退出码：

```bash
node 04_tests/domains/C-order-fulfillment/verify-playwright-evidence.mjs --suite=domains/C-order-fulfillment
```
