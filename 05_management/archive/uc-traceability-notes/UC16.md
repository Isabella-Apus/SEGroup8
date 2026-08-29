# UC16 追溯矩阵

| 验收点 | 生产实现 | 自动化测试 | Evidence |
|---|---|---|---|
| 真实分类、发布并持久化 | `CategoryController`、`createSellerProduct` | `realCategoryCreateEditShelfAndReload_arePersistedAcrossHttpAndDatabase`、Playwright UC16 | MySQL Surefire、Playwright JSON/HTML、截图 |
| 编辑与刷新后数据一致 | `updateSellerProduct`、管理页面编辑对话框 | 同上 | MySQL 数据库断言、`uc16-product-persisted-off-shelf.png` |
| 上架、下架和公开可见性 | `changeSellerProductStatus` | 同上 | Surefire XML、浏览器/API 重查 |
| 删除后不可见 | `deleteSellerProduct` | `ownerDeleteRemovesProductFromSellerPublicAndDetailQueries`、Playwright UC16 | Surefire XML、`uc16-product-deleted-after-refresh.png` |
| 非本人不能编辑、上下架或删除 | `getSellerOwnedProduct` | `nonOwnerCannotEditShelfOrDelete_andProductRemainsUnchanged`、Playwright第三方删除 | HTTP 403 业务响应、数据库不变断言 |
| 已售商品不可修改或重新上架 | `SOLD=3`、`ensureProductMutable` | `soldProductCannotBeEditedRelistedOrDeleted` | H2/MySQL Surefire XML |
| 名称、图片、分类、成色、价格边界 | `SecondhandProductSaveRequest`、分类/价格校验 | `invalidNameImagesCategoryConditionNegotiableAndPrices_doNotWriteProducts` | 11 个拒绝请求及无新增记录断言 |

## 链接

- Domain 设计：[D-secondhand](../domains/D-secondhand/domain-design.md)
- UC16 测试计划：[test-plan.md](../../04_tests/UC16/test-plan.md)
- UC16 测试报告：[test-report.md](../../04_tests/UC16/test-report.md)
