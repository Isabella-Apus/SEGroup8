# UC17 测试报告

## 当前结论

- MySQL Integration：`API_PASS`
- 真实 Compose Playwright：`PASS`

## 已执行结果

| 命令 | Tests | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| `mvn -Dtest=SecondhandDirectPurchaseIntegrationTest test` | 7 | 7 | 0 | 0 |
| `mvn -Dtest=SecondhandProductServiceImplTest,SecondhandProductControllerUc17WebMvcTest test` | 7 | 7 | 0 | 0 |
| `playwright test e2e/domain-d/uc17-direct-purchase.spec.ts --workers=1` | 1 | 1 | 0 | 0 |

覆盖商品状态、地址归属、两买家并发、三表事务一致性、订单写失败回滚、取消/支付、重复购买、议价价格和有效期边界。

浏览器用例访问 `http://127.0.0.1:8088` 的真实 Nginx，后端读取 Compose MySQL seed；数据库、后端、前端 healthcheck 均通过。原始 Surefire XML、服务日志、Playwright JSON/JUnit/HTML 和截图保存在 `evidence/`。
