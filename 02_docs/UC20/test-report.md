# UC20 测试报告

## 当前结论

- MySQL Integration：`API_PASS`
- 真实 Compose Playwright：`PASS`
- 全量后端回归：`PASS`

## 已执行结果

| 命令 | Tests | Passed | Failed | Skipped |
| --- | ---: | ---: | ---: | ---: |
| UC20 Controller + Integration + 兼容流程 + Service 定向回归 | 15 | 15 | 0 | 0 |
| UC20 + UC13/UC23 重复收货契约回归 | 24 | 24 | 0 | 0 |
| `mvn clean verify` | 203 | 203 | 0 | 0 |
| `playwright test e2e/domain-d/uc20-fulfillment.spec.ts --workers=1` | 1 | 1 | 0 | 0 |

后端覆盖卖家归属、付款状态和订单状态校验，重复发货幂等、物流轨迹唯一、买家确认收货、担保资金只结算一次、结算失败整体回滚、通知失败隔离，以及议价成交建单失败回滚。重复确认收货继续遵守项目既有契约，返回业务码 `400`，同时不会重复给卖家入账；UC13 与 UC23 的相关兼容测试一并通过。

浏览器用例访问 `http://127.0.0.1:8088` 的真实 Nginx。测试创建真实 MySQL 二手商品和订单，完成买家付款、卖家页面发货、买家查看物流、确认收货、进入待评价，以及卖家个人钱包增加一次成交金额的完整闭环。数据库、后端和前端 healthcheck 均通过，Playwright 结果为 1/1，耗时约 51.6 秒。

Surefire XML、Compose/服务日志、Playwright JSON/JUnit/HTML 和四张关键截图保存在 `evidence/`。全量 Maven 控制台中的部分连接警告来自 Testcontainers 关闭后后台定时任务的退出阶段；最终 Maven 退出码为 0，且 60 份 Surefire 报告合计 203 个测试均为零失败、零错误。
