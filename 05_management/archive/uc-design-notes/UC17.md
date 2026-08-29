# UC17 二手直接购买

## 范围

买家从在售二手商品详情选择本人收货地址并创建待付款订单。系统必须保证商品、订单和订单明细原子一致，并在并发、重复点击和写入失败时避免重复成交。

## 核心规则

- 仅状态为 `1`（在售）且不存在进行中拍卖的商品可直接购买。
- 买家不能购买自己的商品，地址必须存在且属于当前买家。
- 商品状态通过条件更新 `status = 1 -> 3` 抢占，两个买家只能有一个成功。
- 成交订单初始为 `PENDING_PAY(0)`；支付后为 `PENDING_SHIP(1)`。
- 未付款取消会关闭订单并将二手商品恢复为在售。
- 有效议价必须已生效、未过期、未使用，且成交价大于 0 且不高于当前售价。

## 实现与验证入口

- 服务：`SecondhandProductServiceImpl.buySecondhandProduct`
- 取消/支付：`OrderServiceImpl.cancelMyOrder`、`payMyOrder`
- Integration：`SecondhandDirectPurchaseIntegrationTest`
- E2E：`frontend/e2e/domain-d/uc17-direct-purchase.spec.ts`
- 测试证据：`04_tests/UC17/evidence/`

父用例：#56；本阶段 Task：#149；Epic：#37。
