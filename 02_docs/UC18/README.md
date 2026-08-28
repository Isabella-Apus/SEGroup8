# UC18 议价申请、确认和拒绝

## 范围

买家可针对支持议价的在售二手商品提交合法报价，商品卖家在聊天页查看后只能确认或拒绝一次。确认并建单时，议价、商品、订单和明细必须保持一致；聊天、通知和实时推送属于提交后的辅助动作，失败不得撤销核心决定。

## 核心规则

- 仅在售且 `is_negotiable = 1` 的商品可议价，买家不得向自己报价。
- 报价和确认价必须大于 0 且不高于商品当前售价。
- 同一买家存在 `APPLIED` 或仍有效的 `CONFIRMED` 记录时不得重复申请。
- 只有商品卖家可确认或拒绝；条件更新 `status = APPLIED` 保证并发时只有一个决定成功。
- 确认并建单后议价为 `USED`，商品为已售 `3`，订单为 `PENDING_PAY(0)`，订单价格等于确认价。
- 聊天卡片、通知和实时推送在核心事务提交后尽力执行，异常只记录日志。
- `/bargain/list` 仅返回当前登录用户作为买家或卖家的议价记录。

## 实现与验证入口

- 服务：`SecondhandTradeServiceImpl`
- API：`SecondhandTradeController`
- Integration：`SecondhandNegotiationIntegrationTest`
- E2E：`frontend/e2e/domain-d/uc18-bargain.spec.ts`
- 测试证据：`04_tests/UC18/evidence/`

父用例：#57；本阶段 Task：#150；Epic：#37。
