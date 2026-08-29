# UC11 测试计划

## 测试目标

验证 `POST /api/order/create` 的 JWT 身份、参数校验、服务端计价、地址与交易权限、库存和优惠券事务一致性、幂等回放，以及真实浏览器购物车到订单详情链路。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 场景 | 核心断言 |
|---|---|---|---|
| `UNIT-TC11-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/OrderServiceImplTest.java#payMyOrder_shouldSplitCartItemsIntoIndependentPaidOrders` | 共享 Domain-C 服务回归验证支付拆单，订单按购物车商品独立生成并保持金额一致 |
| `INT-TC11-001` | HTTP + DB Integration | `backend/src/test/java/com/segroup8/platform/integration/OrderCreateUc11IntegrationTest.java#createsOrderFromServerPrice_mergesItems_andPersistsResponseConsistently`；合法商品、本人地址、无券和重复项合并 | 响应、`order_info`、`order_item`、服务端价格和库存一致 |
| `INT-TC11-002` | HTTP + DB Integration | `#rejectsInvalidProductAddressSelfPurchaseAndBlockRelationships_withoutSideEffects`；下架、缺货、地址越权、自购和双向拉黑 | 明确拒绝，订单和库存无副作用 |
| `INT-TC11-003` | HTTP + DB Integration | `#rejectsInvalidRequestParameters_beforeWritingBusinessData`；空商品和非法数量 | DTO 校验拒绝且不写业务数据 |
| `INT-TC11-004` | HTTP + DB Integration | `#appliesEligibleVoucher_andKeepsOrderInventoryAndVoucherConsistent`；合法店铺券 | 折扣、承担额、应付金额和券占用一致 |
| `INT-TC11-005` | HTTP + DB Integration | `#rejectsUnclaimedThresholdAndShopMismatchVouchers_andRollsBackEverything`；未领取、门槛不足、店铺不匹配和不存在的券 | 拒绝并回滚订单、库存和券 |
| `INT-TC11-006` | HTTP + DB Integration | `#laterItemPersistenceFailure_rollsBackOrderInventoryAndOccupiedVoucher`；后续商品失败 | 先前库存修改、订单主从和券全部回滚 |
| `INT-TC11-007` | HTTP + DB Integration | `#duplicateIdempotencyKey_replaysResponseAndCreatesOnlyOneOrder`；相同幂等键重复提交 | 响应回放，只建一单且只扣一次库存 |
| `E2E-TC11-001` | Playwright + MySQL | `frontend/e2e/domain-c/uc11-checkout-order.spec.ts#persists cart checkout and renders the same order after refresh`；商品加购、购物车结算、订单详情、刷新 | 创建响应成功，详情与重新查询的数据持久一致 |

## 本地验证

Domain-C 标签入口用于快速回归并生成统一汇总：

```powershell
node 04_tests/domains/C-order-fulfillment/run-domain-c-tests.mjs --suite UC11
```

同一测试类连接 `compose.yml` 初始化的 MySQL 8.4.6，作为真实数据库 Integration：

```powershell
docker compose up -d database
Push-Location backend
mvn '-Dtest=OrderCreateUc11IntegrationTest' `
  '-Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver' `
  '-Dspring.datasource.url=jdbc:mysql://127.0.0.1:3307/segroup8_platform' `
  '-Dspring.datasource.username=segroup8' `
  '-Dspring.datasource.password=segroup8_dev_password' `
  '-Dspring.sql.init.mode=never' test
Pop-Location
docker compose down --remove-orphans
```

真实浏览器链路复用统一 Compose/Playwright 入口：

```powershell
$env:COMPOSE_FILE = "compose.yml;04_tests/domains/C-order-fulfillment/compose.e2e.yml"
$env:DOMAIN_C_SUITE = 'UC11'
$env:E2E_EVIDENCE_ROOT = '04_tests/UC11/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC11/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-c/uc11-checkout-order.spec.ts --workers=1
```

任何命令失败必须返回非零退出码。只有实际生成的 Surefire、Playwright、日志、截图和 `result-summary.json` 可作为通过证据。
