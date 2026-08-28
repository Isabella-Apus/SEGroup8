# UC24 测试报告

执行时间：2026-08-28 19:04 至 19:07（Asia/Shanghai）

## 结果

| 检查 | 通过 | 失败 | 跳过 | 结果 |
|---|---:|---:|---:|---|
| UC24 后端测试 | 11 | 0 | 0 | PASS |
| 完整 `DOMAIN_E` 后端回归 | 27 | 0 | 0 | PASS |
| UC24 Microsoft Edge Playwright | 1 | 0 | 0 | PASS |
| 前端生产构建 | 1 | 0 | 0 | PASS |

Playwright 在真实 Compose 栈执行，浏览器访问 Nginx 前端 `http://127.0.0.1:8088`，前端经 `/api` 调用 Spring Boot，后端连接 MySQL。测试耗时 6.0 秒，其中浏览器用例耗时 4.7 秒。

## 已验证

- 买家和卖家围绕同一商品重复创建时得到同一个会话，双方列表正确，第三方列表隔离；
- 买家发送、卖家读取并回复，买家刷新后仍能看到双方消息；
- 接收方读取历史后未读状态更新，通知标题和角色对应的消息跳转地址正确；
- 第三方直接读取或发送返回业务码 403，历史数量不变；
- 空白、1001 字符、买家拉黑卖家和卖家拉黑买家均不写消息或通知；
- 页面完成拉黑后，另一方发送返回业务码 403；
- 模拟 `RealtimePushService` 异常时，API 仍返回已持久化消息，消息和通知没有回滚；
- 前端生产构建完成，Compose 的 frontend、backend、database 保持 healthy。

## 环境说明

本机 MySQL 命名卷创建时间早于仓库加入 `third` 测试账号。首次准备 E2E 时按仓库 `docker/mysql/02-seed.sql` 的现有测试夹具补入该账号，随后真实 Edge 流程通过。这是本地持久卷数据版本问题，没有修改业务逻辑或生产数据配置。

UC 覆盖脚本已经识别 `frontend/e2e/domain-e/uc24-chat.spec.ts` 为 `COVERED UC24`。全队清单仍因主线缺少 UC20、UC22、UC23、UC25 spec 返回退出码 1；这些缺口不属于 UC24 分支。

整体状态：`PASS`。原始 Surefire XML、Playwright JSON/XML/HTML、截图和健康检查保存在 `04_tests/UC24/evidence/`。
