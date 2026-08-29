# UC24 测试计划

目标：验证会话幂等创建、参与者隔离、双向消息、已读、通知、拉黑、输入边界、实时推送容错和真实浏览器持久化。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC24-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/ChatServiceImplTest.java#createConversation_shouldAllowSecondhandSellerToContactBuyer` | 合法二手交易双方可以创建会话。 |
| `UNIT-TC24-002` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/UC24ChatAuthorizationTest.java#unitUc24001_nonParticipantCannotReadMessages`; `#unitUc24002_nonParticipantCannotSendMessage` | 非参与者不能读取或发送消息。 |
| `MVC-TC24-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/ChatControllerUc24WebMvcTest.java#currentUserCreatesAndListsTheSameConversation`; `#participantReadsAndSendsMessageWithRealtimeDelivery`; `#realtimeFailureDoesNotTurnAPersistedMessageIntoAnApiFailure`; `#blankAndOversizedMessagesAreRejectedBeforeTheServiceWrites` | 会话/消息路由、实时失败隔离及空白/超长消息校验正确。 |
| `INT-TC24-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/ChatFlowUc24IntegrationTest.java#duplicateCreationReturnsOneConversationToBothParticipantsOnly`; `#bidirectionalHistoryReadStateNotificationsAndIsolationPersist`; `#invalidContentAndEitherDirectionBlockLeaveNoMessage`; `#realtimeFailureKeepsTheMessageAndNotificationCommitted` | 会话幂等、双向历史/已读/通知、拉黑和实时故障下的核心事务一致。 |
| `E2E-TC24-001` | Browser E2E | `frontend/e2e/domain-e/uc24-chat.spec.ts#buyer and seller exchange persisted messages while outsider and blocks are isolated` | 真实页面完成双方消息持久化，并验证旁观者与拉黑隔离。 |

运行命令：

```powershell
cd backend
mvn.cmd -B --no-transfer-progress -Dgroups=UC24 test
mvn.cmd -B --no-transfer-progress -Dgroups=DOMAIN_E test

cd ../frontend
$env:E2E_BROWSER_CHANNEL = "msedge"
$env:E2E_OUTPUT_DIR = "../04_tests/UC24/evidence/raw-reports/playwright"
$env:E2E_BUYER_USERNAME = "user"
$env:E2E_BUYER_PASSWORD = "user123"
$env:E2E_OFFICIAL_SELLER_USERNAME = "seller"
$env:E2E_OFFICIAL_SELLER_PASSWORD = "seller123"
$env:E2E_THIRD_PARTY_USERNAME = "third"
$env:E2E_THIRD_PARTY_PASSWORD = "third123"
npx.cmd playwright test e2e/domain-e/uc24-chat.spec.ts --workers=1
npm.cmd run build
```

这些账号来自 `docker/mysql/02-seed.sql`，只用于本地和 CI。浏览器入口固定为 `http://127.0.0.1:8088`，由 Nginx 代理 `/api` 到真实 Spring Boot 和 MySQL。测试不使用页面 API mock。
