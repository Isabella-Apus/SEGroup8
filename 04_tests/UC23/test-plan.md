# UC23 测试计划

目标：验证钱包初始化、个人充值、经营结算、流水字段、权限隔离、事务回滚、并发更新、重复结算和退款守恒。

| 层级 | 场景 | 自动化入口 | 状态 |
|---|---|---|---|
| Unit | 个人与经营账户更新分离；结算策略 | `UC23AccountIsolationTest`、`EscrowSettlementServiceTest` | 已执行 |
| Controller | 看板、充值参数、经营流水角色 | `FinanceControllerUc23WebMvcTest` | 已执行 |
| Integration | 初始化、充值、真实 H2 流水、事务回滚、并发、订单结算、重复结算、退款守恒 | `FinanceSettlementUc23IntegrationTest` | 已执行 |
| Browser E2E | 买家充值并刷新；真实订单确认收货；卖家查看经营流水并刷新 | `frontend/e2e/domain-e/uc23-wallet-settlement.spec.ts` | 已执行 |

运行命令：

```powershell
cd backend
mvn.cmd -B --no-transfer-progress -Dgroups=UC23 test
mvn.cmd -B --no-transfer-progress -Dgroups=DOMAIN_E test

cd ..\frontend
$env:E2E_BASE_URL = 'http://127.0.0.1:8088'
$env:E2E_OUTPUT_DIR = '..\04_tests\UC23\evidence\raw-reports\playwright'
npx.cmd playwright test e2e/domain-e/uc23-wallet-settlement.spec.ts --workers=1
```

Playwright 使用 `frontend/playwright.config.ts`、团队共享 fixture 和当前 Compose 环境。测试账号取本地非生产 seed 或对应 `E2E_*` 环境变量。
