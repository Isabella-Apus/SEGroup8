# UC18 测试报告

## 当前结论

- MySQL Integration：`API_PASS`
- 真实 Compose Playwright：`PASS`

## 已执行结果

| 命令 | Tests | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `mvn -Dtest=SecondhandNegotiationIntegrationTest test` | 7 | 7 | 0 | 0 |
| `mvn -Dtest=SecondhandTradeServiceImplTest,SecondhandTradeControllerUc18WebMvcTest test` | 7 | 7 | 0 | 0 |
| `mvn clean verify` | 110 | 110 | 0 | 0 |
| `playwright test e2e/domain-d/uc18-bargain.spec.ts --workers=1` | 1 | 1 | 0 | 0 |

后端覆盖真实写库、不可议价、自购、非法/超价/零负数金额、重复有效议价、非卖家权限、确认/拒绝并发、确认建单一致性、拒绝以及聊天/通知失败隔离。

浏览器用例访问 `http://127.0.0.1:8088` 的真实 Nginx；买家从真实商品详情发起 ¥76 议价，卖家在工作台聊天页查看并同意生成订单，买家刷新后看到待付款入口。数据库、后端和前端 healthcheck 均通过。Surefire XML、Compose/服务日志、Playwright JSON/JUnit/HTML 和三张关键截图保存在 `evidence/`。
