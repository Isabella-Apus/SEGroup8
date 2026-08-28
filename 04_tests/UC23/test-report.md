# UC23 测试报告

执行时间：2026-08-28 18:28（Asia/Shanghai）

后端 UC23 定向结果：12 个测试通过，0 失败，0 跳过。`DOMAIN_E` 回归共 28 个测试通过，0 失败，0 跳过。

浏览器结果：真实 Compose 栈上 1 个 Playwright 用例通过，0 失败，0 跳过。用例耗时 3.4 秒，总耗时 4.6 秒。Edge 访问 Nginx 前端 `http://127.0.0.1:8088`，前端经 `/api` 调用 Spring Boot 和 MySQL。

已验证：

- 首次查询创建 0 余额账户；充值只增加个人余额，并写入 `PERSONAL / RECHARGE` 流水；
- 当前用户只能看到本人个人流水，普通用户访问经营流水得到业务码 403；
- 官方商品确认收货只增加卖家经营余额，流水保存订单号、`BUSINESS / INCOME_BUSINESS`、金额和结算后余额；
- 流水插入失败时事务回滚余额；两个并发入账都保留金额和流水；
- 重复确认收货被订单状态和版本条件拒绝，不重复入账；
- 退款从经营账户扣回并写回个人账户，两条退款流水金额和为 0；
- 买家在页面充值，刷新后余额和个人流水保持；卖家财务看板刷新后仍显示本次订单结算。

整体状态：`PASS`。Surefire XML、Playwright JSON/XML/HTML、运行状态和截图保存在 `04_tests/UC23/evidence/`。

静态覆盖清单已识别 `frontend/e2e/domain-e/uc23-wallet-settlement.spec.ts` 为 UC23 的唯一规范路径。全仓覆盖脚本仍会因 `origin/main` 尚缺 UC20、UC22、UC24、UC25 而返回非零；这些用例不属于本 PR 的范围，其中 UC22 正在独立 PR 中等待合并。
