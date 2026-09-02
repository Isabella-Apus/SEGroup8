# UC14 订单售后退款测试报告

## 测试目标

验证售后状态机、买卖双方及管理员权限、资金回流、库存回补、并发幂等和审计日志。UC14 的数据库证据必须来自生产 `schema.sql` 初始化的真实 MySQL，历史 H2 流程仅作为共享回归。

## 自动化范围

| 编号 | 层级 | 场景 | 关键断言 |
|---|---|---|---|
| `INT-TC14-001` | MySQL Integration | 买家申请、卖家拒绝、再次申请、管理员同意 | 状态正确；日志动作、处理人、角色、理由和顺序完整 |
| `INT-TC14-002` | MySQL Integration | 管理员拒绝 | 订单保持原业务状态；无退款流水 |
| `INT-TC14-003` | MySQL Integration | 待发货仅退款自动通过 | 买家到账；订单关闭；新品库存仅回补一次 |
| `INT-TC14-004` | MySQL Integration | 仅退款/退货退款边界 | 非法模式及运输中退货退款拒绝且数据不变；退货退款不自动回补库存 |
| `INT-TC14-005` | MySQL + HTTP | 买家、卖家、管理员权限 | 非本人买家、非商品卖家、非管理员均拒绝且无资金副作用 |
| `INT-TC14-006` | MySQL Integration | 已结算订单退款 | 买家回流、卖家扣回；两条退款流水金额净和为零 |
| `INT-TC14-007` | MySQL Integration | 卖家/管理员并发同意 | 仅一方成功；仅一笔退款和一条同意日志 |
| `WEB-UC14-001/002` | WebMvc | 管理端参数和日志查询合约 | 非法参数拒绝；日志响应字段完整 |
| `E2E-TC14-001/002` | Playwright | 买家申请到卖家处理、管理员仲裁 | 刷新后售后状态和处理记录仍持久化 |

## 数据库与隔离

- 容器：`mysql:8.4.6`。
- 初始化：`backend/src/main/resources/schema.sql`。
- Fixture：`backend/src/test/resources/integration/uc14-refund-setup.sql`。
- 主测试：`OrderRefundUc14IntegrationTest`，标记 `DOMAIN_C` 和 `UC14`。
- `OrderAfterSaleIntegrationTest`、`OrderSettlementRefundFlowIntegrationTest` 标记为 `PLATFORM`，不计入 UC14 真实数据库统计。

## 验证命令

```powershell
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs `
  --suite UC14 `
  --maven-repository "$HOME/.m2/repository"
```

## 本次结果

- 执行时间：2026-08-27。
- 结果：`PASS`。
- Tests：9；Failures：0；Errors：0；Skipped：0。
- 全量后端：`mvn clean verify`，102 tests，0 failures，0 errors，0 skipped。
- 前端：`npm run build:real`，构建成功（存在既有 chunk size warning）。
- Surefire：`evidence/raw-reports/surefire/`。
- 汇总：`evidence/result-summary.json`。
- 浏览器 E2E：当前完整系统流水线 33526387696 已在 Compose/MySQL 环境复验通过，完整报告由 Actions artifact 保存。

2026-08-27 对当前提交发起的浏览器复验在 `compose-build` 阶段被外部镜像源阻塞：DaoCloud 返回 EOF，Playwright 未启动，因此没有把该次尝试记为 PASS，也没有覆盖已有浏览器原始报告。

## 当前结论

本地 MySQL Integration 与当前 `main` 的 Playwright、全量后端和前端构建均已通过；早期镜像代理失败仅作为历史排查背景，不影响当前结论。
