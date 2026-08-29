---
name: UC16-UC20 二手交易交付
about: 二手发布、购买、议价、拍卖和履约的实现与验收
title: "[UC16-UC20] 完成二手发布、购买、议价、拍卖和履约"
labels: enhancement, testing, documentation
assignees: ""
---

## 目标

完成 UC16-UC20 的用例、三层模型、接口与数据归属、自动化测试、前端演示截图和评审证据。

## 验收条件

- [ ] UC16-UC20 用例说明完整，每个用例具有系统级、组件级、对象级图
- [ ] 二手商品发布、编辑、上下架、删除权限正确
- [ ] 二手直接购买生成待付款订单，支付后进入待发货
- [ ] 议价支持买家发起、卖家同意/拒绝，同意后生成待付款订单
- [ ] 拍卖支持卖家发起、买家出价、卖家查看监控
- [ ] 二手订单支持卖家发货、买家确认收货和待评价闭环
- [ ] 后端 D 相关测试通过
- [ ] 前端 UI/E2E 截图覆盖 UC16-UC20 主要流程
- [ ] 二手分类树 mock 与真实接口路径一致

## 证据

- 分支：`codex/uc16-uc20-secondhand`
- 文档：`02_docs/UC16-UC20-用例说明与三层模型.md`
- 测试报告：`05_management/archive/test-assets/UC16-UC20/UC16-UC20-测试报告.md`
- 验收检查表：`05_management/archive/test-assets/UC16-UC20/UC16-UC20-用例验收检查表.md`
- 演示证据：`05_management/UC16-UC20-评审与演示证据.md`
- 后端测试命令：`cd backend && mvn -B "-Dtest=SecondhandProductServiceImplTest,SecondhandTradeServiceImplTest,SecondhandOrderFlowIntegrationTest,SecondhandAuctionIntegrationTest" test`
