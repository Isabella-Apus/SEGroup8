# UC25 测试计划

目标：验证通知所有权、范围筛选、已读状态、WebSocket 鉴权、实时投递和断线补偿。

| 层级 | 场景 | 自动化入口 |
|---|---|---|
| Controller | 当前用户、scope 参数、单条和批量已读路由、统一 404 | `NotificationControllerUc25WebMvcTest` |
| Service | 他人通知拒绝、持久化后只推送归属用户 | `UC25NotificationOwnershipAndPushTest` |
| Integration | 本人列表、buyer/seller 筛选、单条/范围/全部已读、推送异常持久化 | `NotificationFlowUc25IntegrationTest` |
| WebSocket | 有效、缺失、篡改、过期 JWT | `RealtimeHandshakeInterceptorTest` |
| Browser E2E | 登录、真实连接、实时显示、已读刷新持久化、断网后重连补拉 | `frontend/e2e/domain-e/uc25-notification.spec.ts` |

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
