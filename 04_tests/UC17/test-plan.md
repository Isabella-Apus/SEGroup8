# UC17 测试计划

## 目标

验证二手直接购买的状态、地址、并发、事务、取消、支付、重复提交和议价价格边界。

## 层次

| 层次 | 环境 | 场景 |
| --- | --- | --- |
| Controller | MockMvc | 请求体校验和待付款响应 |
| Integration | Spring Boot + Testcontainers MySQL 8.4.6 | 7 个数据库/API 场景，含并发和失败注入 |
| E2E | Docker Compose + Nginx + Spring Boot + MySQL + Chromium | 详情选地址、下单、刷新、订单一致性和重复购买拒绝 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress -Dtest=SecondhandDirectPurchaseIntegrationTest test

cd ../frontend
npx playwright test e2e/domain-d/uc17-direct-purchase.spec.ts --workers=1
```

完整真栈由 `scripts/e2e/run-compose-e2e.ps1` 启动并收集失败证据。
