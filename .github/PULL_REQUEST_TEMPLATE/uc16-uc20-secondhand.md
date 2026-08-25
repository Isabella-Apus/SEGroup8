## 变更摘要

- 完成 UC16-UC20 用例说明及 15 张三层模型图
- 补充二手发布、购买、议价、拍卖、履约的接口与数据归属说明
- 新增 UC16-UC20 追溯矩阵、demo.http、测试报告和评审演示证据
- 新增按组长检查字段整理的 UC16-UC20 用例验收检查表
- 补充 26 张前端 UI/E2E 截图证据，并修复 UC16 二手分类树 mock 缺失问题

关联 Issue：#请替换为实际 Issue 编号（如无对应 Issue 可删除本行）

## 验证

- [x] `cd backend && mvn -B "-Dtest=SecondhandProductServiceImplTest,SecondhandTradeServiceImplTest,SecondhandOrderFlowIntegrationTest,SecondhandAuctionIntegrationTest" test`
- [x] `cd frontend && npm run build:mock`
- [x] 前端 Vite mock 环境完成 UC16-UC20 走查
- [x] `05_management/UC16-UC20-screenshots` 共 26 张截图可打开
- [x] `04_tests/UC16-UC20-ui-run-result.json` 可解析
- [x] 未提交真实密码、Token 或云平台密钥

## 评审重点

- 买家不能购买、议价或竞拍自己的闲置
- 议价同意后订单是否先进入待付款，而不是直接待发货
- 拍卖是否正确处理历史拍卖、当前价、最低出价和领先买家
- 二手订单发货、物流、确认收货和待评价状态是否闭环
- 前端 mock 路由与后端接口是否一致，特别是二手分类树

