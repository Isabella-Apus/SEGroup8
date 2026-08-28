# UC19 测试计划

## 目标

验证二手拍卖从创建、竞价、资金冻结与退款到流拍或成交结算的身份、时间、金额、并发、幂等和事务一致性。

## 层次

| 层次 | 环境 | 场景 |
| --- | --- | --- |
| Controller/Service 回归 | MockMvc + Mockito | 路由契约、拍卖创建、竞价和结算基础行为 |
| Integration | Spring Boot + Testcontainers MySQL 8.4.6 | 7 个真实数据库/API 场景，含并发、失败注入和重试 |
| E2E | Docker Compose + Nginx + Spring Boot + MySQL + Chromium | 卖家创建、两名买家出价、卖家监控与结束、赢家查看订单 |

## 自动化场景

| 编号 | 场景 | 核心断言 |
| --- | --- | --- |
| `INT-UC19-001` | 创建、历史拍卖、重复进行中拍卖和非本人商品 | 只有合法创建写入一场 `ONGOING` 拍卖 |
| `INT-UC19-002` | 两名买家依次合法出价 | 两条日志、前一名退款、后一名冻结正确 |
| `INT-UC19-003` | 不存在、未开始、已结束、过期和卖家自购 | 全部拒绝且余额/日志不变 |
| `INT-UC19-004` | 两个请求并发出价 | 仅一个成功、一个最高出价人和一笔冻结 |
| `INT-UC19-005` | 非卖家结束与无出价结束 | 越权拒绝；流拍不创建订单 |
| `INT-UC19-006` | 有出价结束并重复触发结算 | 一个已付款待发货订单、一个明细、商品已售 |
| `INT-UC19-007` | 写订单明细时失败后重试 | 首次整体回滚，重试只创建一个订单 |
| `E2E-UC19-001` | 真实页面完整竞拍 | 卖家/两买家页面、余额、竞价数、成交订单一致 |

## 命令

```powershell
cd backend
mvn -B --no-transfer-progress "-Dtest=SecondhandAuctionLifecycleIntegrationTest,SecondhandTradeServiceImplTest,SecondhandTradeControllerUc19WebMvcTest" test
mvn -B --no-transfer-progress clean verify

cd ..
$env:DOMAIN_D_SUITE = 'UC19'
$env:E2E_EVIDENCE_ROOT = '04_tests/UC19/evidence'
$env:E2E_OUTPUT_DIR = '04_tests/UC19/evidence/raw-reports/playwright'
.\scripts\e2e\run-compose-e2e.ps1 -ResetDatabase -- e2e/domain-d/uc19-auction.spec.ts --workers=1
```

统一脚本严格按 database -> backend -> frontend 启动，主动等待 Docker healthcheck 和 HTTP health。命令失败必须返回 Playwright 的真实非零退出码，并保留 HTML、JSON、JUnit、截图、trace、video 和服务日志。
