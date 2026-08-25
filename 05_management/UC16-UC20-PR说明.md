# PR 标题

[UC16-UC20] 完成二手商城发布、购买、议价、拍卖与履约交付

# PR 说明

## 变更摘要

- 完成成员 D 负责的 UC16-UC20 二手商城交付材料：用例说明、三层模型、接口与数据归属、追溯矩阵、验收检查表、测试报告和评审证据。
- 修复前端 mock 缺失 `/category/tree?scene=SECONDHAND` 的问题，二手发布页分类树可以正常加载。
- 新增 `SecondhandTradeServiceImplTest`，覆盖议价同意生成待付款订单、议价拒绝、重复处理拦截、拍卖低价拦截、结束后出价拦截、拍卖结算幂等。
- 补充 26 张 UC16-UC20 前端 UI/E2E 截图证据，覆盖发布、购买、议价、拍卖、履约闭环。
- 补充 D 部分第 2 天个人附加任务：k6 压测脚本框架和 reset-all 数据重置方案，用于后续单体/微服务性能对比。
- 新增 UC16-UC20 专用 Issue 模板、PR 模板和 GitHub Actions 工作流，方便组长按统一格式检查。

## 验证结果

- [x] `cd backend && mvn -B "-Dtest=SecondhandProductServiceImplTest,SecondhandTradeServiceImplTest,SecondhandOrderFlowIntegrationTest,SecondhandAuctionIntegrationTest" test`
  - Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
- [x] `cd frontend && npm run build:mock`
- [x] Playwright 回归打开二手发布页，分类下拉显示“数码闲置”
- [x] `04_tests/UC16-UC20-ui-run-result.json` 可解析
- [x] `05_management/UC16-UC20-screenshots` 共 26 张截图
- [x] `04_tests/performance/k6/` 已提供商品搜索、新品下单、二手拍卖出价/购买脚本框架
- [x] `04_tests/UC16-UC20-k6压测框架与数据重置方案.md` 已说明 reset-all 数据重置流程
- [x] 未提交真实密码、Token 或云平台密钥

## 交付证据

- `02_docs/UC16-UC20-用例说明与三层模型.md`
- `02_docs/UC16-UC20-接口与数据归属.md`
- `02_docs/UC16-UC20-追溯矩阵.md`
- `04_tests/UC16-UC20-测试报告.md`
- `04_tests/UC16-UC20-用例验收检查表.md`
- `04_tests/UC16-UC20-UI运行记录.md`
- `04_tests/UC16-UC20-ui-run-result.json`
- `04_tests/UC16-UC20-k6压测框架与数据重置方案.md`
- `04_tests/performance/k6/`
- `05_management/UC16-UC20-评审与演示证据.md`
- `05_management/UC16-UC20-screenshots/`

## 评审重点

- UC16 二手发布：分类、价格、成色、议价开关、拍卖发布和上下架状态是否一致。
- UC17 二手直接购买：自购阻断、库存/状态变更、订单创建和支付前后状态是否正确。
- UC18 二手议价：买家发起议价后卖家可同意或拒绝；同意后创建待付款订单，付款后才进入待发货。
- UC19 二手拍卖：卖家可发起并查看拍卖情况；买家可查看拍卖状态并出价；低价、结束后出价和重复结算会被拦截。
- UC20 二手订单履约：卖家发货、物流信息、买家确认收货和待评价状态形成闭环。

## 备注

关联 Issue 编号需要在 GitHub 上创建或确认后回填；如果暂时没有对应 Issue，可以先提交 PR，再由组长补充关联。
