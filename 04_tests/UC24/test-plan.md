# UC24 测试计划

目标：验证会话幂等创建、参与者隔离、双向消息、已读、通知、拉黑、输入边界、实时推送容错和真实浏览器持久化。

| 层级 | 场景 | 自动化入口 | 状态 |
|---|---|---|---|
| Unit / Controller | 当前用户传递、输入校验、实时推送异常 | `ChatControllerUc24WebMvcTest`、`ChatServiceImplTest`、`UC24ChatAuthorizationTest` | 已执行 |
| API / Integration | 幂等创建、列表隔离、双向历史、已读、通知、第三方 403、双向拉黑、无脏写 | `ChatFlowUc24IntegrationTest` | 已执行 |
| Browser E2E | 买家创建并发送，卖家回复，刷新持久化，第三方越权，页面拉黑后拒发 | `frontend/e2e/domain-e/uc24-chat.spec.ts` | 已执行 |

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
