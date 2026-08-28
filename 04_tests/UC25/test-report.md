# UC25 测试报告

执行时间：2026-08-28 22:14（Asia/Shanghai）

后端 UC25 结果：14 个测试通过，0 失败，0 跳过。完整 `DOMAIN_E` 回归为 28 个测试通过，0 失败，0 跳过。

浏览器结果：真实 Compose 栈上 1 个本机 Edge Playwright 用例通过，0 失败，0 跳过。浏览器访问 Nginx 前端 `http://127.0.0.1:8088`，前端通过 `/api` 和 `/ws/realtime` 连接 Spring Boot 与 MySQL。

已验证：

- 列表只返回当前用户通知，buyer/seller scope 筛选正确；
- 单条、范围和全部已读写库正确，其他用户的通知不受影响；
- 他人通知和不存在通知都返回业务码 404；
- 通知创建只推送归属用户，推送运行时异常不撤销已落库记录；
- 有效 JWT 的 WebSocket 握手通过，缺失、篡改和过期 JWT 均返回 401；
- Edge 登录后建立真实 WebSocket，通知无需刷新即可显示；
- 标记已读后刷新页面仍保持已读；
- 显式断开 WebSocket 并保持浏览器离线，在断线期间创建通知，恢复网络后客户端重新连接并自动补拉遗漏通知。

覆盖清单脚本已识别 `frontend/e2e/domain-e/uc25-notification.spec.ts` 为 UC25 的唯一规范位置；同步最新 `origin/main` 后，UC01-UC25 的规范位置检查全部通过。

整体状态：`PASS`。原始 JUnit XML、Playwright JSON/XML/HTML、截图和运行健康记录位于 `04_tests/UC25/evidence/`。
