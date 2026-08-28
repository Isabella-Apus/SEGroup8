# UC20 测试计划

## 目标

验证二手成交订单从付款、卖家发货、物流可见、买家确认收货到卖家个人钱包结算的权限、状态、幂等、事务和故障隔离，并确认收货后的用户界面停留在待评价状态。

## 层次

| 层次 | 环境 | 场景 |
| --- | --- | --- |
| Controller/Service 回归 | MockMvc + Mockito | 稳定路由、服务状态机和既有二手发货兼容性 |
| Integration | Spring Boot + Testcontainers MySQL 8.4.6 | 6 个真实数据库/API 场景，含权限、幂等、结算、通知失败和事务回滚 |
| E2E | Docker Compose + Nginx + Spring Boot + MySQL + Chromium | 已付款订单、卖家发货、买家物流与收货、待评价、钱包只入账一次 |

## 自动化场景

| 编号 | 场景 | 核心断言 |
| --- | --- | --- |
| `INT-UC20-001` | 非卖家、未付款、错误状态发货 | 全部拒绝，订单和物流不变化 |
| `INT-UC20-002` | 合法发货后重复发货 | 订单保持已发货，仅一条首物流轨迹 |
| `INT-UC20-003` | 非买家确认、合法确认和重复确认 | 越权拒绝；合法收货进入待评价；重复确认返回 400 且卖家仅入账一次 |
| `INT-UC20-004` | 发货/收货通知抛异常 | 主交易仍提交，订单状态正确 |
| `INT-UC20-005` | 结算服务首次失败后重试 | 首次整体回滚；重试只有一笔卖家入账 |
| `INT-UC20-006` | 议价成交建单失败 | 商品和议价状态回滚，无残缺订单 |
| `E2E-UC20-001` | 真实页面履约闭环 | 卖家发货、买家看物流并收货、订单待评价、重复确认被拒且不重复结算 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress "-Dtest=SecondhandFulfillmentLifecycleIntegrationTest,OrderControllerUc20WebMvcTest,SecondhandOrderFlowIntegrationTest,OrderServiceImplTest" test
mvn -B --no-transfer-progress clean verify

cd ..
$env:DOMAIN_D_SUITE = 'UC20'
$env:E2E_EVIDENCE_ROOT = '04_tests/UC20/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC20/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-d/uc20-fulfillment.spec.ts --workers=1
```

统一脚本按 database -> backend -> frontend 启动，并等待 Docker healthcheck、后端 HTTP health 和前端 HTTP health。Playwright 失败必须返回真实非零退出码，阻断后续 deploy/release，并保留 HTML、JSON、JUnit、截图、trace、video 与 Compose 服务日志。
