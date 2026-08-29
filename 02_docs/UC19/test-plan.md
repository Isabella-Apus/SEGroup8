# UC19 测试计划

## 目标

验证二手拍卖从创建、竞价、资金冻结与退款到流拍或成交结算的身份、时间、金额、并发、幂等和事务一致性。

## 层次

| 层次 | 环境 | 场景 |
| --- | --- | --- |
| Controller/Service 回归 | MockMvc + Mockito | 路由契约、拍卖创建、竞价和结算基础行为 |
| Integration | Spring Boot + Testcontainers MySQL 8.4.6 | 7 个真实数据库/API 场景，含并发、失败注入和重试 |
| E2E | Docker Compose + Nginx + Spring Boot + MySQL + Chromium | 卖家创建、两名买家出价、卖家监控与结束、赢家查看订单 |

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 场景与核心断言 |
| --- | --- | --- | --- |
| `UNIT-TC19-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/SecondhandTradeServiceImplTest.java#placeBid_shouldRejectBidLowerThanMinimumIncrement`; `#placeBid_shouldRejectEndedAuction`; `#settleExpiredAuctions_shouldCreateOnlyOneOrderForAlreadySettledAuction` | 出价边界、结束状态和结算幂等。 |
| `MVC-TC19-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/SecondhandTradeControllerUc19WebMvcTest.java#auctionCreateQueryAndBid_shouldExposeSellerAndBuyerRoutes` | 拍卖创建、查询和竞价路由契约。 |
| `INT-TC19-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/SecondhandAuctionLifecycleIntegrationTest.java#sellerCanCreateAfterHistoricalAuctionButDuplicateAndNonOwnerAreRejected` | 只有合法创建写入一场 `ONGOING` 拍卖。 |
| `INT-TC19-002` | Integration | `#legalBidsPersistLogsAndReleaseThePreviousBidderFunds` | 两条日志、前一名退款、后一名冻结正确。 |
| `INT-TC19-003` | Integration | `#nonexistentFutureClosedExpiredAndSelfBidsAreRejected` | 不存在、未开始、已结束、过期和卖家自购全部拒绝且余额/日志不变。 |
| `INT-TC19-004` | Integration | `#concurrentBidsLeaveExactlyOneLeaderAndOneFundHold` | 并发竞价只保留一个成功领先者和一笔冻结。 |
| `INT-TC19-005` | Integration | `#onlySellerCanCloseAndNoBidAuctionFlowsWithoutAnOrder` | 非卖家结束被拒；流拍不创建订单。 |
| `INT-TC19-006` | Integration | `#sellerCloseCreatesOnePaidPendingShipmentOrderAndItem` | 结束后只产生一个已付款待发货订单和一条明细。 |
| `INT-TC19-007` | Integration | `#failedSettlementRollsBackAndCanRetryWithoutDuplicateOrder` | 写订单明细失败时整体回滚，重试只创建一个订单。 |
| `E2E-TC19-001` | Browser E2E | `frontend/e2e/domain-d/uc19-auction.spec.ts#seller creates an auction, two buyers bid, and the winner receives one settled order` | 卖家/两买家页面、余额、竞价数和成交订单一致。 |

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
