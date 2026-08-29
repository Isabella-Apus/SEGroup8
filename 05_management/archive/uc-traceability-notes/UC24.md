# UC24 需求追溯

| REQ | SYS / COMP / OBJ | Code / Table | Test | Report / Evidence | 状态 |
|---|---|---|---|---|---|
| REQ24-01 会话创建幂等、双方列表正确 | `system.mmd`、`component.mmd`、`object.mmd` | `ChatServiceImpl`、`chat_conversation` | `ChatFlowUc24IntegrationTest.duplicateCreationReturnsOneConversationToBothParticipantsOnly`、UC24 Edge E2E | `04_tests/UC24/test-report.md`、Playwright 报告 | PASS |
| REQ24-02 双向消息、历史、已读与通知 | 同上 | `ChatController`、`ChatServiceImpl`、`chat_message`、`notification` | `bidirectionalHistoryReadStateNotificationsAndIsolationPersist`、UC24 Edge E2E | JUnit XML、截图、Playwright JSON/HTML | PASS |
| REQ24-03 空白、超长和双向拉黑不写库 | `system.mmd`、`component.mmd` | `ChatMessageSendRequest`、`UserBlockMapper`、`chat_message` | `invalidContentAndEitherDirectionBlockLeaveNoMessage`、`ChatControllerUc24WebMvcTest`、UC24 Edge E2E | JUnit XML、Playwright 报告 | PASS |
| REQ24-04 第三方不能查看或发送 | 三张设计图 | `requireConversationParticipant`、`chat_conversation` | `bidirectionalHistoryReadStateNotificationsAndIsolationPersist`、UC24 Edge E2E | JUnit XML、Playwright 报告 | PASS |
| REQ24-05 推送失败不回滚消息 | `system.mmd`、`component.mmd` | `ChatController.sendMessage`、`RealtimePushService`、`chat_message`、`notification` | `realtimeFailureKeepsTheMessageAndNotificationCommitted`、Controller WebMvc 测试 | JUnit XML、运行日志 | PASS |
| REQ24-06 买家、卖家、第三方真实浏览器隔离 | `system.mmd` | `ChatView.vue`、`chat.js`、Nginx、Spring Boot、MySQL | `frontend/e2e/domain-e/uc24-chat.spec.ts` | Edge 截图及 JSON/XML/HTML | PASS |
