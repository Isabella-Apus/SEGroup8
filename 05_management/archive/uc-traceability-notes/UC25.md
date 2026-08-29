# UC25 追溯

| 需求 | 设计 | 实现 | 自动化验证 |
|---|---|---|---|
| 本人通知与 scope 筛选 | `system.mmd`、`component.mmd` | `NotificationController`、`NotificationServiceImpl` | `NotificationControllerUc25WebMvcTest`、`NotificationFlowUc25IntegrationTest` |
| 单条、全量、范围已读 | `component.mmd` | `markRead`、`markAllRead` | `NotificationFlowUc25IntegrationTest` |
| 他人及不存在通知不泄露 | `component.mmd` | 归属校验与统一 404 | Controller 与 Integration 测试 |
| 写库后定向推送，推送失败保留记录 | `component.mmd`、`object.mmd` | `createNotification`、`RealtimePushService` | `UC25NotificationOwnershipAndPushTest`、Integration 测试 |
| JWT WebSocket 鉴权 | `system.mmd`、`object.mmd` | `RealtimeHandshakeInterceptor` | `RealtimeHandshakeInterceptorTest` |
| 浏览器实时接收、已读持久化 | `component.mmd` | `realtimeClient.js`、`NotificationView.vue` | `uc25-notification.spec.ts` |
| 重连列表补偿 | `component.mmd` | `REALTIME_RECONNECTED` 后重新查询 | `uc25-notification.spec.ts` 断网场景 |

执行结果和原始报告统一保存于 `04_tests/UC25/evidence/`。
