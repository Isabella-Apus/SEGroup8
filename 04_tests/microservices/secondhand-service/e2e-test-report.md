# MS-04 UC16-UC20 E2E 测试报告

## 环境

- 浏览器：Playwright Chromium
- 被测系统：Docker Compose 中真实 Nginx 前端、Spring Boot 后端和 MySQL seed 数据
- 测试源码：仅引用 `frontend/e2e/domain-d/`，未复制 spec

## 执行记录

| 轮次 | 结果 | 说明 |
|---|---|---|
| 首次全量 | 4 passed / 1 failed | UC18 后端已返回成功，但页面将已确认议价从待处理列表中丢失，断言未看到“已生成订单” |
| UC18 定向复测 | 1 passed / 0 failed | 修复 `ChatView.vue` 的本地列表合并逻辑后通过 |
| 最终全量 | 5 passed / 0 failed | UC16、UC17、UC18、UC19、UC20 全部通过，耗时约 89.1 秒 |

## 最终用例

- UC16：发布、审核后在售、上下架、非所有者拒绝、删除后刷新仍生效。
- UC17：直接购买生成唯一待付款订单，商品售出，重复购买被拒绝。
- UC18：买家议价、卖家确认、买家看到待付款订单。
- UC19：创建拍卖、两次有效出价、卖家结算、赢家获得唯一订单。
- UC20：发货、物流可见、确认收货、待评价和卖家只结算一次。

最终机器报告：`evidence/playwright-final/playwright-results.json` 与 `playwright-results.xml`。HTML 报告保留在同目录，命名截图归档在 `evidence/screenshots/`；首次失败的截图、上下文、trace 和 video 归档在 `evidence/raw-reports/playwright-failure/`，用于回归问题追踪。
