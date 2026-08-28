# UC07 卖家商品生命周期测试报告

## 当前结果

| 层级 | 状态 | 口径 |
| --- | --- | --- |
| 服务单元 | `PASS` | 当前 `ProductServiceImplTest` 目标运行记录为 5 条通过、0 失败、0 错误。 |
| 浏览器 Playwright E2E | `PENDING` | 文件已存在，但本轮没有重新执行 Compose/MySQL 浏览器流程。 |

## 服务单元覆盖

- `pagePublicProducts_shouldThrowWhenPriceRangeInvalid`
- `getPublicProductDetail_shouldThrowWhenProductNotOnShelf`
- `createSellerProduct_shouldPersistWithCurrentSellerShopId`
- `adjustSellerProductStock_shouldThrowWhenResultNegative`
- `pageSellerProducts_shouldUseCurrentSellerShopIdAndReturnPageVO`

这些测试覆盖价格边界、公开下架过滤、当前店铺绑定、负库存拒绝和卖家列表隔离。创建/编辑/上下架/删除的完整浏览器业务链路仍须后续在真实 Compose 栈中复核。

## 证据边界

历史本地浏览器记录曾覆盖商品创建、编辑、库存、下架、删除和买家授权，但它不是本轮新执行结果；不能用该历史记录替代当前 CI 或新 Compose E2E 证据。测试计划见 [test-plan.md](test-plan.md)，需求/代码追踪见 [UC07 追溯矩阵](../../02_docs/UC07-卖家商品生命周期-追溯矩阵.md)。
