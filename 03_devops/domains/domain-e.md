# Domain-E 测试与 CI 入口

Domain-E 对应 Epic #38 与 UC21–UC25。共享测试基础设施 Task 为 #147。本文件只定义测试入口和证据边界，不替代各 UC 的需求、设计、测试计划与报告。

## 后端标签与范围

- 所有 Domain-E 测试使用 `@Tag("DOMAIN_E")`。
- 每个测试同时使用一个或多个 `UC21`–`UC25` 标签。
- `DOMAIN_E` 统计只计算优惠券、钱包/结算、聊天、通知和实时推送测试；平台 smoke 与其他 Domain 测试不计入。

定向运行：

```bash
cd backend
mvn -B -Dgroups=DOMAIN_E test
mvn -B -Dgroups=UC21 test
```

完整回归仍使用：

```bash
cd backend
mvn -B clean verify
```

Surefire XML 位于 `backend/target/surefire-reports/`。CI 的 `DOMAIN_E tagged tests` job 会上传 `domain-e-surefire-reports` artifact。

## Integration Test 边界

UC Integration 必须经过 Controller、鉴权、Service、Mapper 与真实测试数据库，并验证持久化状态。MockMvc 组件测试和 Mockito 单元测试不能单独宣称 UC Integration 完成。未实际执行的场景在 traceability/report 中标记 `PENDING`。

## 统一 Playwright + Compose

共享 Playwright/Compose 脚手架已由 PR #133 合并。Domain-E 直接复用：

- `frontend/playwright.config.ts`
- `frontend/e2e/fixtures/index.ts`
- `frontend/e2e/helpers/`
- `compose.yml` + `compose.e2e.yml`
- `scripts/e2e/run-compose-e2e.sh`
- `.github/workflows/ci-cd.yml` 中的 `Real full-stack Playwright E2E`

禁止新建 Cypress、第二套 Playwright 配置或 Domain-E 专用 Compose 框架。

定向运行：

```bash
cd frontend
npm ci
npm run e2e:domain-e
```

完整真实环境运行：

```bash
COMPOSE_FILE=compose.yml:compose.e2e.yml scripts/e2e/run-compose-e2e.sh
```

各 UC spec 放在 `frontend/e2e/domain-e/`，并从 `../fixtures` 导入共享 `test/expect`。

Compose seed 提供 `admin`、`seller`、`user`、`third` 四个非生产测试账号；CI 通过 `E2E_ADMIN_*`、`E2E_OFFICIAL_SELLER_*`、`E2E_BUYER_*` 和 `E2E_THIRD_PARTY_*` 传入凭据。测试代码必须通过共享 `loginAs` fixture 获取账号，不得硬编码生产凭据。

## Evidence 输出

- 平台级 Playwright 原始输出：`04_tests/platform-e2e/evidence/`
- Domain-E 汇总入口：`04_tests/domains/E-engagement-finance/`
- UC 证据：`04_tests/UC21/evidence/` 至 `04_tests/UC25/evidence/`
- 每个 UC 至少保留 `logs/`、`raw-reports/`、`screenshots/` 与 `result-summary.json`。

平台 smoke 通过只证明脚手架可用，不代表任一 UC 完成。只有实际执行真实浏览器业务链、生成报告和 Evidence 后才能在 UC 报告中写 `PASS`。
