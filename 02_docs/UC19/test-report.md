# UC19 测试报告

## 当前结论

- MySQL Integration：`API_PASS`
- 真实 Compose Playwright：`PASS`

## 已执行结果

| 命令 | Tests | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| UC19 Controller + Integration + Service 定向回归 | 14 | 14 | 0 | 0 |
| `mvn clean verify` | 161 | 161 | 0 | 0 |
| `playwright test e2e/domain-d/uc19-auction.spec.ts --workers=1` | 1 | 1 | 0 | 0 |

后端覆盖拍卖创建、重复拍卖、权限、时间和金额边界、卖家自购、资金冻结/退款、竞价日志、并发冲突、流拍、成交结算、幂等和失败回滚重试。

浏览器用例访问 `http://127.0.0.1:8088` 的真实 Nginx。卖家为真实 MySQL 商品发起 ¥50 起拍、¥5 加价的拍卖；买家 A 出价 ¥50，买家 B 出价 ¥60；卖家工作台看到 2 次出价及最高出价人后提前结束。系统为买家 B 创建唯一的已付款、待发货二手订单。数据库、后端和前端 healthcheck 均通过。

Surefire XML、Compose/服务日志、Playwright JSON/JUnit/HTML 和四张关键截图保存在 `evidence/`。全量 Maven 调试控制台会展开本机配置，因此不作为提交证据；最终结论以不含本机 Secret 的 Surefire XML、Playwright 报告和机器汇总为准。
