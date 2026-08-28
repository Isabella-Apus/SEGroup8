# UC18 测试计划

## 目标

验证二手议价从申请到确认/拒绝的资格、金额、重复、权限、并发、事务一致性和辅助系统故障隔离。

## 层次

| 层次 | 环境 | 场景 |
| --- | --- | --- |
| Controller/Service 回归 | MockMvc + Mockito | 稳定路由、确认建单、拒绝和已处理状态 |
| Integration | Spring Boot + Testcontainers MySQL 8.4.6 | 7 个真实数据库/API 场景，含并发和失败注入 |
| E2E | Docker Compose + Nginx + Spring Boot + MySQL + Chromium | 买家申请、卖家聊天页确认、买家刷新、待付款订单校验 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress -Dtest=SecondhandNegotiationIntegrationTest test
mvn -B --no-transfer-progress "-Dtest=SecondhandTradeServiceImplTest,SecondhandTradeControllerUc18WebMvcTest" test

cd ..\frontend
npx playwright test e2e/domain-d/uc18-bargain.spec.ts --workers=1
```

完整真栈由 `scripts/e2e/run-compose-e2e.ps1` 按 database -> backend -> frontend 顺序启动，并收集浏览器报告、截图和三端日志。
