# UC 追溯矩阵

| UC | 接口/能力 | 自动化证据 |
|---|---|---|
| UC11 | 创建、拆单快照、库存预留、报价 | `OrderApiTest` create/list/detail |
| UC12 | 支付、取消、支付超时查询 | `OrderApiTest`、`PaymentFailureContractTest` |
| UC13 | 卖家查询、发货、物流、确认收货与写接口幂等 | `OrderApiTest` 主流程及重复请求断言 |
| UC14 | 退款申请、卖家/管理员决定、资金反向流水 | `OrderApiTest` refund/admin |
| UC15 | 整单/逐项评价、追评、卖家查询/回复 | `OrderApiTest` review flow |
| UC20 | businessKey 二手创建、统一履约、确认收货单次结算 | `OrderApiTest` internal secondhand/settlement；`DownstreamHttpContractTest` settlement contract |

浏览器 E2E 继续复用 `frontend/e2e/domain-c/uc11..uc15` 与 `frontend/e2e/domain-d/uc20-fulfillment.spec.ts`，不复制 spec。
