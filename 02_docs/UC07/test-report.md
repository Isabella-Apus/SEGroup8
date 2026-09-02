# UC07 卖家商品生命周期测试报告

## 当前结果

| 层级 | 状态 | 口径 |
| --- | --- | --- |
| 服务单元 | `PASS` | 当前 `ProductServiceImplTest` 目标运行记录为 5 条通过、0 失败、0 错误。 |
| 浏览器 Playwright E2E | `PASS` | `main@b622e6bb` 完整系统流水线已通过 UC01-UC25 Playwright；当前运行 33526387696。 |

## 服务单元覆盖

- `pagePublicProducts_shouldThrowWhenPriceRangeInvalid`
- `getPublicProductDetail_shouldThrowWhenProductNotOnShelf`
- `createSellerProduct_shouldPersistWithCurrentSellerShopId`
- `adjustSellerProductStock_shouldThrowWhenResultNegative`
- `pageSellerProducts_shouldUseCurrentSellerShopIdAndReturnPageVO`

这些测试覆盖价格边界、公开下架过滤、当前店铺绑定、负库存拒绝和卖家列表隔离。创建、编辑、上下架、删除的完整浏览器业务链路已在真实 Compose/MySQL 栈中复核。

## 证据边界

当前验收以 [完整系统流水线 33526387696](https://github.com/Isabella-Apus/SEGroup8/actions/runs/33526387696) 和 Actions artifact 为准。测试计划见 [test-plan.md](test-plan.md)，需求/代码追踪见 [UC07 追溯矩阵](traceability.md)。
