# UC25 测试计划

目标：验证通知所有权、范围筛选、已读状态、WebSocket 鉴权、实时投递和断线补偿。

## 自动化测试清单

编号规则统一为：`UNIT-TCxx` 表示 Service/规则单元测试，`MVC-TCxx` 表示
Controller 的 MockMvc/API 契约测试，`INT-TCxx` 表示 Spring Boot HTTP + 数据库
集成测试，`E2E-TCxx` 表示 Compose + MySQL + Playwright 真浏览器测试。

| 编号 | 层级 | 实际入口（文件#方法） | 验证内容与核心断言 |
|---|---|---|---|
| `UNIT-TC25-001` | Unit | `backend/src/test/java/com/segroup8/platform/service/impl/UC25NotificationOwnershipAndPushTest.java#markRead_shouldRejectNotificationOwnedByAnotherUser`; `#createNotification_shouldPersistThenPushOnlyToOwner` | 他人通知不能标记已读；通知先持久化，再只向归属用户推送。 |
| `UNIT-TC25-002` | Unit/WebSocket | `backend/src/test/java/com/segroup8/platform/realtime/RealtimeHandshakeInterceptorTest.java#beforeHandshake_shouldAcceptValidTokenAndExposeUserId`; `#beforeHandshake_shouldRejectMissingToken`; `#beforeHandshake_shouldRejectTamperedToken`; `#beforeHandshake_shouldRejectExpiredToken` | WebSocket 握手接受有效 JWT，并拒绝缺失、篡改和过期 Token。 |
| `MVC-TC25-001` | API/MockMvc | `backend/src/test/java/com/segroup8/platform/controller/NotificationControllerUc25WebMvcTest.java#list_shouldUseCurrentUserAndScope`; `#markRead_shouldUseCurrentUserAndNotificationId`; `#markAllRead_shouldPassOptionalScope`; `#foreignNotification_shouldKeepUnifiedNotFoundResponse` | 当前用户、scope、单条/批量已读和统一 404 路由契约正确。 |
| `INT-TC25-001` | Integration | `backend/src/test/java/com/segroup8/platform/integration/NotificationFlowUc25IntegrationTest.java#list_shouldIsolateCurrentUserAndApplyBuyerOrSellerScope`; `#markRead_shouldUpdateOwnedNotificationAndHideForeignOrMissingOnes`; `#markAllRead_shouldSupportScopedAndUnscopedUpdates`; `#create_shouldPushOnlyOwnerAndKeepRecordWhenPushFails` | 通知列表隔离、范围筛选、单条/批量已读和推送失败持久化一致。 |
| `E2E-TC25-001` | Browser E2E | `frontend/e2e/domain-e/uc25-notification.spec.ts#buyer receives, reads and recovers missed notifications in real Edge` | 真实 Edge 完成登录、WebSocket 实时显示、已读刷新和断线重连补拉。 |

本机执行：

```powershell
cd backend
mvn.cmd -B --no-transfer-progress -Dgroups=UC25 test

cd ../frontend
$env:E2E_BROWSER_CHANNEL='msedge'
$env:E2E_BASE_URL='http://127.0.0.1:8088'
$env:E2E_OUTPUT_DIR='../04_tests/UC25/evidence/raw-reports/playwright'
npx.cmd playwright test e2e/domain-e/uc25-notification.spec.ts --workers=1
```

完整 Domain E 回归另执行 `mvn.cmd -B --no-transfer-progress -Dgroups=DOMAIN_E test`。浏览器测试前确认 Compose 三个服务均为 healthy。
