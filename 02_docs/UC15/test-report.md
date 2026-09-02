# UC15 订单评价、追评与回复 API 测试报告

## 测试目标

验证订单级首评、逐商品评价、事务回滚、一次追评、实际商品卖家回复、买卖双方分页查询隔离和订单状态一致性。

## 自动化场景

| 编号 | 层级 | 场景 | 核心断言 |
|---|---|---|---|
| `UT-UC15-001` | Service | 订单级多商品首评 | 每个订单项写入一条 ORIGINAL，订单从待评价变为已完成 |
| `UT-UC15-002/003` | Service | 重复首评、订单外商品 | 请求被拒绝，评价和订单状态不产生副作用 |
| `API-TC15-001` | MySQL Integration | 首评、回复、追评闭环 | 仅实际卖家可回复；追评以首评为前置且最多一次 |
| `API-TC15-002` | MySQL Integration | 多商品中途写入失败 | 第一条已写入后第二条触发数据库异常，整个事务回滚且订单仍待评价 |
| `API-TC15-003` | WebMvc | 参数和卖家空集合 | 响应结构正确，无商品卖家不会执行无约束评价查询 |
| `API-TC15-004` | MySQL Integration | 买家/卖家分页隔离 | `total` 与分页记录一致，不返回其他买家或其他卖家的评价 |
| `E2E-TC15-001` | Playwright | 买家首评、卖家回复、买家追评 | 刷新或重新查询后评价和订单状态仍持久化 |

## 验证命令

```powershell
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs --suite UC15 --maven-repository C:/Users/29382/.m2/repository
```

## 本次执行结果

- 执行时间：2026-08-27。
- JUnit Tag：`DOMAIN_C & UC15`。
- 数据库：Testcontainers MySQL 8.4.6，加载生产 `backend/src/main/resources/schema.sql`。
- 结果：9 tests passed，0 failures，0 errors，0 skipped。
- Maven 退出码：0。
- Surefire 后缀：`domain-c-uc15-20260827135302750`。
- 汇总：`evidence/result-summary.json`。
- 原始 JUnit XML：`../../04_tests/UC15/evidence/raw-reports/surefire/`。

## 浏览器证据

`frontend/e2e/domain-c/uc15-review.spec.ts` 覆盖买家首评、卖家回复、买家追评以及刷新后的持久化验证。当前 `main` 完整系统流水线 33526387696 已复验通过，完整浏览器报告由 Actions artifact 保存。

## 回归结果

- Backend `mvn clean verify`：104 tests passed，0 failures，0 errors，0 skipped。
- Frontend `npm run build:real`：构建成功；仅保留已有的大分块体积告警。
- Compose Playwright：当前完整系统流水线通过。
