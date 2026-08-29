# PR 标题

[UC16-UC20] 完成二手商城发布、购买、议价、拍卖与履约交付

# PR 说明

## 变更摘要

- 完成成员 D 负责的 UC16-UC20 二手商城交付材料：用例说明、三层模型、接口与数据归属、追溯矩阵、验收检查表、测试报告和评审证据。
- 修复前端 mock 缺失 `/category/tree?scene=SECONDHAND` 的问题，二手发布页分类树可以正常加载。
- 新增 `SecondhandTradeServiceImplTest`，覆盖议价同意生成待付款订单、议价拒绝、重复处理拦截、拍卖低价拦截、结束后出价拦截、拍卖结算幂等。
- 补充 26 张 UC16-UC20 前端 Vite mock UI walkthrough 截图证据；真实 Compose E2E 不由这些截图证明。
- 新增 UC16-UC20 专用 Issue 模板、PR 模板和 GitHub Actions 工作流，方便组长按统一格式检查。

## 验证结果

- [x] `cd backend && mvn -B --no-transfer-progress "-Dgroups=DOMAIN_D" clean verify`
  - 9 classes; Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 (`API_PASS`)
- [x] `cd frontend && npm run build:mock`
- [x] Playwright 回归打开二手发布页，分类下拉显示“数码闲置”
- [x] `05_management/archive/test-assets/UC16-UC20/UC16-UC20-ui-run-result.json` 可解析
- [x] `05_management/UC16-UC20-screenshots` 共 26 张截图
- [ ] UC16-UC20 五个真实 Compose Playwright 流程（`E2E_PENDING`，由 Tasks #148-#152 分别跟踪）
- [x] 未提交真实密码、Token 或云平台密钥

## 交付证据

- `02_docs/UC16-UC20-用例说明与三层模型.md`
- `02_docs/UC16-UC20-接口与数据归属.md`
- `02_docs/UC16-UC20-追溯矩阵.md`
- `05_management/archive/test-assets/UC16-UC20/UC16-UC20-测试报告.md`
- `05_management/archive/test-assets/UC16-UC20/UC16-UC20-用例验收检查表.md`
- `05_management/archive/test-assets/UC16-UC20/UC16-UC20-UI运行记录.md`
- `05_management/archive/test-assets/UC16-UC20/UC16-UC20-ui-run-result.json`
- `05_management/UC16-UC20-评审与演示证据.md`
- `05_management/UC16-UC20-screenshots/`

## 评审重点

- UC16 二手发布：分类、价格、成色、议价开关、拍卖发布和上下架状态是否一致。
- UC17 二手直接购买：自购阻断、库存/状态变更、订单创建和支付前后状态是否正确。
- UC18 二手议价：买家发起议价后卖家可同意或拒绝；同意后创建待付款订单，付款后才进入待发货。
- UC19 二手拍卖：卖家可发起并查看拍卖情况；买家可查看拍卖状态并出价；低价、结束后出价和重复结算会被拦截。
- UC20 二手订单履约：卖家发货、物流信息、买家确认收货和待评价状态形成闭环。

## 备注

Domain-D 共享测试入口对应 #119；UC16-UC20 的真实流程分别对应 #148-#152。PR 不直接关闭父 UC #55-#59。
